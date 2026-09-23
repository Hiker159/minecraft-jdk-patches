/* SPDX-License-Identifier: GPL-2.0-only */
import org.lwjgl.PointerBuffer;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.freetype.*;
import java.nio.*;
import java.security.MessageDigest;
import java.util.HexFormat;
import static org.lwjgl.util.freetype.FreeType.*;
public class FreeTypeSmoke {
    static void check(int error, String action) {
        if (error != 0) throw new AssertionError(action + ": FreeType error " + error);
    }
    public static void main(String[] args) throws Exception {
        if (args.length == 0) throw new IllegalArgumentException("Supply one or more font paths");
        try (MemoryStack stack = MemoryStack.stackPush()) {
            PointerBuffer pointer = stack.mallocPointer(1);
            check(FT_Init_FreeType(pointer), "initialize");
            long library = pointer.get(0);
            try {
                IntBuffer major=stack.mallocInt(1), minor=stack.mallocInt(1), patch=stack.mallocInt(1);
                FT_Library_Version(library, major, minor, patch);
                System.out.println("FreeType " + major.get(0) + "." + minor.get(0) + "." + patch.get(0));
                for (String font : args) {
                    check(FT_New_Face(library, font, 0, pointer), "open " + font);
                    FT_Face face = FT_Face.create(pointer.get(0));
                    try {
                        check(FT_Set_Pixel_Sizes(face, 0, 24), "pixel size");
                        MessageDigest digest=MessageDigest.getInstance("SHA-256");
                        int nonzero=0;
                        for (int mode : new int[]{FT_LOAD_RENDER, FT_LOAD_RENDER | FT_LOAD_FORCE_AUTOHINT}) {
                            for (int character : "ABC".codePoints().toArray()) {
                                check(FT_Load_Char(face, character, mode), "render glyph");
                                FT_Bitmap bitmap=face.glyph().bitmap();
                                int length=Math.abs(bitmap.pitch())*bitmap.rows();
                                if (length > 0) {
                                    ByteBuffer pixels=bitmap.buffer(length);
                                    for (int i=0;i<length;i++) if (pixels.get(i)!=0) nonzero++;
                                    digest.update(pixels);
                                }
                            }
                        }
                        if (nonzero==0) throw new AssertionError("No visible glyph pixels in " + font);
                        System.out.println("PASS: glyph rendering and auto-hinting: " + font + " SHA256=" + HexFormat.of().formatHex(digest.digest()));
                    } finally { check(FT_Done_Face(face), "release face"); }
                }
            } finally { check(FT_Done_FreeType(library), "release library"); }
        }
    }
}
