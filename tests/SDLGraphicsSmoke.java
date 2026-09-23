/* SPDX-License-Identifier: GPL-2.0-only */
import org.lwjgl.opengl.GL;
import org.lwjgl.sdl.SDL_Event;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.sdl.SDLInit.*;
import static org.lwjgl.sdl.SDLError.*;
import static org.lwjgl.sdl.SDLEvents.*;
import static org.lwjgl.sdl.SDLKeyboard.*;
import static org.lwjgl.sdl.SDLMouse.*;
import static org.lwjgl.sdl.SDLVersion.*;
import static org.lwjgl.sdl.SDLVideo.*;

public class SDLGraphicsSmoke {
    static void check(boolean ok, String action) {
        if (!ok) throw new AssertionError(action + ": " + SDL_GetError());
    }
    public static void main(String[] args) throws Exception {
        boolean fullscreen = args.length > 0 && args[0].equals("--fullscreen");
        System.out.println("Java " + Runtime.version() + ", SDL " + SDL_GetVersion() + " " + SDL_GetRevision());
        check(SDL_Init(SDL_INIT_VIDEO), "SDL video initialization");
        long window = 0, context = 0;
        try {
            check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MAJOR_VERSION, 3), "GL major");
            check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_MINOR_VERSION, 2), "GL minor");
            check(SDL_GL_SetAttribute(SDL_GL_CONTEXT_PROFILE_MASK, SDL_GL_CONTEXT_PROFILE_CORE), "GL core profile");
            check(SDL_GL_SetAttribute(SDL_GL_DOUBLEBUFFER, 1), "double buffering");
            window = SDL_CreateWindow("Mavericks SDL3 test", 640, 400, SDL_WINDOW_OPENGL | SDL_WINDOW_RESIZABLE);
            check(window != 0, "window creation");
            context = SDL_GL_CreateContext(window);
            check(context != 0, "OpenGL context creation");
            check(SDL_GL_MakeCurrent(window, context), "make context current");
            GL.createCapabilities();
            System.out.println("OpenGL " + glGetString(GL_VERSION) + " / " + glGetString(GL_RENDERER));
            check(SDL_SetWindowSize(window, 720, 450), "window resize");
            try (SDL_Event event = SDL_Event.malloc()) {
                long end = System.nanoTime() + 3_000_000_000L;
                if (fullscreen) {
                    check(SDL_SetWindowFullscreen(window, true), "enter fullscreen");
                    check(SDL_SyncWindow(window), "wait for fullscreen");
                    check((SDL_GetWindowFlags(window) & SDL_WINDOW_FULLSCREEN) != 0, "fullscreen flag");
                }
                check(SDL_SetWindowRelativeMouseMode(window, true), "relative mouse mode");
                while (System.nanoTime() < end) {
                    while (SDL_PollEvent(event)) { /* Exercise real Cocoa event delivery. */ }
                    check(SDL_GetKeyboardState().remaining() > 0, "keyboard state");
                    SDL_GetMouseState(null, null);
                    glClearColor(0.1f, 0.3f, 0.7f, 1.0f);
                    glClear(GL_COLOR_BUFFER_BIT);
                    check(SDL_GL_SwapWindow(window), "buffer swap");
                    Thread.sleep(16);
                }
                check(SDL_SetWindowRelativeMouseMode(window, false), "release relative mouse mode");
                if (fullscreen) {
                    check(SDL_SetWindowFullscreen(window, false), "leave fullscreen");
                    check(SDL_SyncWindow(window), "wait for windowed mode");
                    check((SDL_GetWindowFlags(window) & SDL_WINDOW_FULLSCREEN) == 0, "windowed flag");
                    long settle = System.nanoTime() + 1_000_000_000L;
                    while (System.nanoTime() < settle) { SDL_PumpEvents(); Thread.sleep(16); }
                }
            }
            System.out.println("PASS: SDL window, resize, keyboard/mouse queries, relative mouse mode, event polling, OpenGL, swap" +
                               (fullscreen ? ", fullscreen transitions" : ""));
        } finally {
            if (context != 0) SDL_GL_DestroyContext(context);
            if (window != 0) SDL_DestroyWindow(window);
            SDL_Quit();
        }
    }
}
