package aQute.bnd.main;

import java.io.*;
import java.util.*;
import java.util.regex.*;

import aQute.bnd.annotation.metatype.*;
import aQute.bnd.annotation.plugin.*;
import aQute.bnd.build.*;
import aQute.bnd.header.*;
import aQute.lib.getopt.*;

/**
 * Combines plugin management commands
 */
public class Plugins {
	private static final Pattern	PLUGIN_FILE_P	= Pattern.compile("(.*)\\.bnd");
	private Map<String,Class< ? >>	annotatedPlugins;
	private final Workspace			ws;
	private final bnd				bnd;

	Plugins(bnd bnd) throws Exception {
		this.bnd = bnd;
		this.ws = bnd.getWorkspace(bnd.getBase());
	}

	interface PluginAddOptions extends Options {
		String alias();

		boolean force();

		// boolean interactive();
		
		// String jar();
	}

	public void _add(PluginAddOptions opts) throws Exception {
		List<String> args = opts._();
		Map<String,Class< ? >> plugins = getAnnotatedPlugins();

		if (args.isEmpty()) {
			print(plugins);
			return;
		}

		String pluginName = args.remove(0);
		String alias = opts.alias()==null ? pluginName : opts.alias();

		Class< ? > plugin = plugins.get(pluginName);
		if (plugin == null) {
			bnd.error("No such plugin: %s", pluginName);
			return;
		} else {

			Map<String,String> parameters = new HashMap<String,String>();

			for (String parameter : args) {
				if (parameter.indexOf('=') < 1)
					parameter += "=true";

				Attrs attrs = OSGiHeader.parseProperties(parameter);
				parameters.putAll(attrs);
			}

			// if ( false && opts.interactive()) {
			//
			// Interactive interactive = new Interactive(bnd.out, System.in,
			// parameters);
			// if ( !interactive.go() ) {
			// bnd.error("aborted by interactive editor");
			// return;
			// }
			// }

			ws.addPlugin(plugin, alias, parameters, opts.force());
		}
		return;
	}

	@Arguments(arg = "alias...")
	interface PluginRemoveOptions extends Options {}

	public void _remove(PluginRemoveOptions opts) {
		List<String> args = opts._();

		if (args.isEmpty()) {
			for (String file : ws.getFile("cnf/ext").list()) {
				Matcher m = PLUGIN_FILE_P.matcher(file);
				if (m.matches()) {
					bnd.out.println(m.group(1));
				}
			}
			return;
		}

		for (String p : args) {
			ws.removePlugin(p);
		}
	}

	private void print(Map<String,Class< ? >> plugins) {
		bnd.out.printf("%-30s %s%n", "Type", "Description");
		for (Map.Entry<String,Class< ? >> e : plugins.entrySet()) {
			bnd.out.printf("%-30s %s%n", e.getKey(), getDescription(e.getValue()));
		}
	}

	/*
	 * Return the description of the plugin. Requires a plugin that is annotated
	 * with BndPlugin and that has a Config which is annotated with an OCD.
	 */
	private String getDescription(Class< ? > clazz) {
		String description = clazz.getName();

		Meta.OCD ocd = getOCD(clazz);
		if (ocd != null && ocd.description() != Meta.NULL)
			description = ocd.description();

		return description;
	}

	private Meta.OCD getOCD(Class< ? > pluginClass) {
		BndPlugin plugin = pluginClass.getAnnotation(BndPlugin.class);
		if (plugin != null) {
			Class< ? > configClass = plugin.parameters();
			if (configClass != null && configClass != Object.class) {
				Meta.OCD ocd = configClass.getAnnotation(Meta.OCD.class);
				return ocd;
			}
		}
		return null;
	}

	private Map<String,Class< ? >> getAnnotatedPlugins() throws IOException {
		if (annotatedPlugins == null) {
			annotatedPlugins = new TreeMap<String,Class< ? >>();

			InputStream in = bnd.class.getResourceAsStream("bnd.info");
			Properties p = new Properties();
			p.load(in);
			Parameters classes = new Parameters(p.getProperty("plugins"));
			for (String cname : classes.keySet()) {
				try {
					Class< ? > c = getClass().getClassLoader().loadClass(cname);
					aQute.bnd.annotation.plugin.BndPlugin annotation = c
							.getAnnotation(aQute.bnd.annotation.plugin.BndPlugin.class);
					if (annotation != null) {
						annotatedPlugins.put(annotation.name(), c);
					}

				}
				catch (Exception ex) {
					bnd.error("Cannot find plugin %s", cname);
				}
			}

		}
		return annotatedPlugins;
	}

}