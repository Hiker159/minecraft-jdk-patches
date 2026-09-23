/* SPDX-License-Identifier: GPL-2.0-only */
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

/** Run on the first macOS thread with Minecraft's LWJGL 3.3.3 JARs. */
public class MinecraftGraphicsSmoke {
    public static void main(String[] args) {
        GLFWErrorCallback callback = GLFWErrorCallback.createPrint(System.err);
        callback.set();
        long window = 0;
        try {
            if (!glfwInit()) throw new AssertionError("GLFW initialization failed");
            glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
            glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 2);
            glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
            glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);
            window = glfwCreateWindow(360, 180, "Minecraft Java graphics test", 0, 0);
            if (window == 0) throw new AssertionError("OpenGL window creation failed");
            glfwMakeContextCurrent(window);
            GL.createCapabilities();
            System.out.println("Java " + Runtime.version());
            System.out.println("OpenGL " + glGetString(GL_VERSION));
            System.out.println("Renderer: " + glGetString(GL_RENDERER));
            glfwSwapInterval(1);
            long end = System.nanoTime() + 2_000_000_000L;
            while (!glfwWindowShouldClose(window) && System.nanoTime() < end) {
                glClearColor(0.1f, 0.4f, 0.8f, 1.0f);
                glClear(GL_COLOR_BUFFER_BIT);
                glfwSwapBuffers(window);
                glfwPollEvents();
            }
            if (glGetError() != GL_NO_ERROR) throw new AssertionError("OpenGL error");
            System.out.println("PASS: LWJGL 3.3.3, GLFW, OpenGL core context, buffer swap");
        } finally {
            if (window != 0) glfwDestroyWindow(window);
            glfwTerminate();
            glfwSetErrorCallback(null);
            callback.free();
        }
    }
}
