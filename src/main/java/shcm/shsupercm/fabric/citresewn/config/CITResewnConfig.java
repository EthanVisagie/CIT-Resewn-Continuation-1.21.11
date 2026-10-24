package shcm.shsupercm.fabric.citresewn.config;

import com.google.gson.Gson;
import com.google.gson.stream.JsonWriter;
import net.fabricmc.loader.api.FabricLoader;
import org.apache.commons.io.IOUtils;
import shcm.shsupercm.fabric.citresewn.CITResewn;

import java.io.*;

/**
 * Contains runtime representation of CIT Resewn's config, encoded using GSON.
 */
public class CITResewnConfig {
    /**
     * Whether CIT Resewn should work or not.<br>
     * Requires a restart.
     */
    public boolean enabled = true;
    /**
     * Mutes pack loading errors from logs.
     */
    public boolean mute_errors = false;
    /**
     * Mutes pack loading warnings from logs.
     */
    public boolean mute_warns = false;
    /**
     * Invalidating interval for CITs' caches in milliseconds. Set to 0 to disable caching.
     */
    public int cache_ms = 50;
    /**
     * Buckets item rules by the exact {@code custom_data} string they require, so a stack only has to be
     * tested against the few rules that can possibly match it instead of every rule of its item.<br>
     * Set to {@code false} to go back to testing every rule of the item (pre-1.2.2-fork.11 behaviour), which
     * is far slower on packs with hundreds of renamed items but needs no index.
     * Requires a resource reload.
     */
    public boolean item_value_index = true;
    /**
     * Should broken paths be allowed in resourcepacks. Requires a restart.
     * @see BrokenPaths
     */
    public boolean broken_paths = false;

    /**
     * CIT Resewn's config storage file.
     */
    private static final File FILE = new File(FabricLoader.getInstance().getConfigDir().toFile(), "citresewn.json");

    /**
     * Active instance of the current config.
     */
    public static final CITResewnConfig INSTANCE = read();

    /**
     * Reads the stored config.
     * @see #FILE
     * @return the read config
     */
    public static CITResewnConfig read() {
        if (!FILE.exists())
            return new CITResewnConfig().write();

        Reader reader = null;
        try {
            return new Gson().fromJson(reader = new FileReader(FILE), CITResewnConfig.class);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(reader);
        }
    }

    /**
     * Saves this config to file.
     * @see #FILE
     * @return this
     */
    public CITResewnConfig write() {
        Gson gson = new Gson();
        JsonWriter writer = null;
        try {
            writer = gson.newJsonWriter(new FileWriter(FILE));
            writer.setIndent("    ");

            gson.toJson(gson.toJsonTree(this, CITResewnConfig.class), writer);
        } catch (Exception e) {
            CITResewn.LOG.error("Couldn't save config");
            e.printStackTrace();
            throw new RuntimeException(e);
        } finally {
            IOUtils.closeQuietly(writer);
        }
        return this;
    }
}
