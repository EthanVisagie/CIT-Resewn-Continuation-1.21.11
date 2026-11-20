package shcm.shsupercm.fabric.citresewn.pack;

import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import shcm.shsupercm.fabric.citresewn.CITResewn;
import shcm.shsupercm.fabric.citresewn.config.CITResewnConfig;

import javax.imageio.ImageIO;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

public final class CitSpriteResource {
    // Bound the extra heap used to convert a mislabeled image during atlas loading.
    private static final long MAX_JPEG_PIXELS = 4096L * 4096;

    private CitSpriteResource() {}

    public static Resource allowJpeg(Identifier file, Resource resource) {
        AtomicBoolean warned = new AtomicBoolean();
        return new Resource(resource.source(), () -> {
            BufferedInputStream input = new BufferedInputStream(resource.open());
            boolean passThrough = false;
            try {
                input.mark(3);
                boolean jpeg = input.read() == 0xff && input.read() == 0xd8 && input.read() == 0xff;
                input.reset();
                if (!jpeg) {
                    passThrough = true;
                    return input;
                }

                InputStream png = convertJpeg(input);
                if (warned.compareAndSet(false, true) && !CITResewnConfig.INSTANCE.mute_warns)
                    CITResewn.LOG.warn("{} in pack {} contains JPEG data. Loaded it as JPEG. Export the image as PNG for compatibility with other loaders.", file, resource.sourcePackId());
                return png;
            } finally {
                if (!passThrough)
                    input.close();
            }
        }, resource::metadata);
    }

    private static InputStream convertJpeg(InputStream input) throws IOException {
        try (var source = new MemoryCacheImageInputStream(input)) {
            var reader = ImageIO.getImageReadersByFormatName("JPEG").next();
            try {
                reader.setInput(source, true, true);
                if ((long) reader.getWidth(0) * reader.getHeight(0) > MAX_JPEG_PIXELS)
                    throw new IOException("JPEG exceeds the CIT fallback limit of " + MAX_JPEG_PIXELS + " pixels");
                ByteArrayOutputStream png = new ByteArrayOutputStream();
                if (!ImageIO.write(reader.read(0), "PNG", png))
                    throw new IOException("No PNG encoder is available for the CIT JPEG fallback");
                return new ByteArrayInputStream(png.toByteArray());
            } finally {
                reader.dispose();
            }
        }
    }
}
