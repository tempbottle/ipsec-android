package org.za.hem.ipsec_tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.net.InetAddress;
import java.util.AbstractCollection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.content.Context;
import android.os.Environment;
import android.os.Process;

public class ConfigManager {
	
	public static final String PATTERN = "\\$\\{([a-zA-Z0-9_]+)\\}";
	public static final String CONFIG_PREFIX = ".conf";
	public static final String PEERS_CONFIG = "peers.conf";
	public static final String RACOON_HEAD = "racoon.head";
	
	// Variables
	public static final String VAR_BINDIR = "bindir";
	public static final String VAR_EXTDIR = "extdir";
	public static final String VAR_REMOTE_ADDR = "remote_addr";
	public static final String VAR_LOCAL_ADDR = "local_addr";
	public static final String VAR_UID = "uid";
	public static final String VAR_GID = "gid";
	public static final String VAR_NAME = "name";
	

	private Pattern mPat;
	private Map<String,String> mVariables;
	private Context mContext;
	private File mBinDir;
	
	public ConfigManager(Context context) {
		mBinDir = context.getDir("bin", Context.MODE_PRIVATE);
		mVariables = new HashMap<String,String>();
		mVariables.put(VAR_BINDIR, mBinDir.getAbsolutePath());
		mVariables.put(VAR_EXTDIR, Environment.getExternalStorageDirectory().getAbsolutePath());
		mVariables.put(VAR_UID, "" + Process.myUid());
		mVariables.put(VAR_GID, "" + Process.myUid());
		mPat = Pattern.compile(PATTERN);
		mContext = context;
	}
	
	protected File getPeerConfigFile(Peer peer) {
		return new File(mBinDir, peer.getPeerID().key + CONFIG_PREFIX);
	}
	
	protected void buildPeerConfig(Peer peer, Writer os) throws IOException {
		InetAddress addr = peer.getRemoteAddr();
		if (addr != null)
			mVariables.put(VAR_REMOTE_ADDR, addr.getHostAddress());
		mVariables.put(VAR_LOCAL_ADDR, peer.getLocalAddr().getHostAddress());
		mVariables.put(VAR_NAME, peer.getName());
		File tmpl = peer.getTemplateFile();
		if (tmpl == null)
			return;
		PolicyFile policy = new PolicyFile(tmpl);
		substitute(policy.getRacoonConfStream(), os);
	}
	
	protected File buildPeerConfig(Peer peer) throws IOException {
 		mVariables.put(VAR_LOCAL_ADDR, peer.getLocalAddr().getHostAddress());
		File output = getPeerConfigFile(peer);
		FileWriter os = new FileWriter(output);
		buildPeerConfig(peer, os);
		os.close();
		return output;
	}
	
	public void build(AbstractCollection<Peer> peers,
			boolean updateAllPeers) throws IOException {
		Iterator<Peer> iter = peers.iterator();
		Writer out = new FileWriter(new File(mBinDir, PEERS_CONFIG));
		Reader inHead = new InputStreamReader(mContext.getAssets().open(RACOON_HEAD));
		substitute(inHead, out);
		while (iter.hasNext()) {
			Peer peer = iter.next();
			if (peer == null)
				continue;
			if (!peer.isEnabled())
				continue;
			mVariables.remove(VAR_REMOTE_ADDR);
			mVariables.remove(VAR_LOCAL_ADDR);
			mVariables.remove(VAR_NAME);
			try {
				File output;
				if (updateAllPeers)
					output = buildPeerConfig(peer);
				else
					output = getPeerConfigFile(peer);
				out.write("include \"" + output.getAbsolutePath() + "\";\n");
			} catch (IOException e){
			}
		}
		out.close();
		// build peers.conf
	}
	
	public void addVariable(String key, String value) {
		mVariables.put(key, value);
	}

	protected String substituteLine(String line) {
		StringBuffer buf = new StringBuffer();
		Matcher m = mPat.matcher(line);

		while (m.find()) {
			String var = m.group(1);
			String value;
			
			if (mVariables.containsKey(var)) {
				value = mVariables.get(var);
			} else {
				value = "";
			}

			m.appendReplacement(buf, value);
		}
		m.appendTail(buf);
		buf.append('\n');

		return buf.toString();
	}
	
	public void substitute(Reader input, Writer os) {
		BufferedReader is = new BufferedReader(input);
			
		try {
			String line;
			while ( (line = is.readLine()) != null) {
				os.write(substituteLine(line));
			}				
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
		}
	}

	public void substitute(java.io.InputStream input, Writer os) {
		substitute(new InputStreamReader(input), os);
	}

	public void substitute(File input, Writer os) {
		Reader is = null;
		try {
			is = new FileReader(input);
			substitute(is, os);
		} catch (IOException e) {
			throw new RuntimeException(e);
		} finally {
			try {
				if (is != null)
					is.close();
			} catch (IOException e) {
			}
		}
	}
}
