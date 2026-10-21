package com.krolasyon.film;

import com.mojang.blaze3d.pipeline.RenderTarget;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/** Ana çerçeve tamponunu her karede okuyup ham RGB olarak ffmpeg'e (x264) borular. */
final class Recorder {
    private static final byte[] END = new byte[0];
    private Process proc;
    private BlockingQueue<byte[]> queue;
    private Thread writer;
    private ByteBuffer buf;
    private int w, h;
    long frames;

    boolean started() { return proc != null; }

    void start(File out, int width, int height, int fps) throws IOException {
        w = width;
        h = height;
        buf = BufferUtils.createByteBuffer(w * h * 3);
        out.getParentFile().mkdirs();
        proc = new ProcessBuilder("ffmpeg", "-y", "-loglevel", "error", "-f", "rawvideo", "-pix_fmt", "rgb24", "-s", w + "x" + h,
                "-r", String.valueOf(fps), "-i", "-", "-vf", "vflip", "-c:v", "libx264", "-preset", "medium", "-crf", "20",
                "-pix_fmt", "yuv420p", out.getAbsolutePath())
                .redirectErrorStream(true).redirectOutput(new File(out.getParentFile(), "ffmpeg_video.log")).start();
        queue = new ArrayBlockingQueue<>(6);
        OutputStream os = new BufferedOutputStream(proc.getOutputStream(), 1 << 20);
        writer = new Thread(() -> {
            try {
                while (true) {
                    byte[] f = queue.take();
                    if (f == END) break;
                    os.write(f);
                }
                os.close();
            } catch (Exception e) {
                Director.LOG.error("[FILM] video writer failed", e);
            }
        }, "film-writer");
        writer.setDaemon(true);
        writer.start();
    }

    void frame(RenderTarget rt) {
        if (proc == null) return;
        rt.bindWrite(false);
        buf.clear();
        GL11.glPixelStorei(GL11.GL_PACK_ALIGNMENT, 1);
        GL11.glReadPixels(0, 0, w, h, GL11.GL_RGB, GL11.GL_UNSIGNED_BYTE, buf);
        byte[] b = new byte[w * h * 3];
        buf.get(b);
        try {
            queue.put(b);
        } catch (InterruptedException ignored) {
        }
        frames++;
    }

    void finish() {
        if (proc == null) return;
        try {
            queue.put(END);
            writer.join(600_000);
            proc.waitFor();
        } catch (InterruptedException ignored) {
        }
        Director.LOG.info("[FILM] video finished, frames {}", frames);
        proc = null;
    }
}
