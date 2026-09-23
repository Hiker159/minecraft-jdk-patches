/* SPDX-License-Identifier: GPL-2.0-only */
import java.nio.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.zip.ZipFile;
import org.lwjgl.system.MemoryUtil;
import org.lwjgl.util.shaderc.*;
import static org.lwjgl.util.shaderc.Shaderc.*;
public class ShadercSmoke {
    static void compile(long compiler,long options,String name,String source,int kind,boolean valid) throws Exception {
        long result=shaderc_compile_into_spv(compiler,source,kind,name,"main",options);
        if(result==0)throw new AssertionError("No compilation result");
        try {
            int status=shaderc_result_get_compilation_status(result);
            String errors=shaderc_result_get_error_message(result);
            if(valid) {
                if(status!=shaderc_compilation_status_success)throw new AssertionError(name+": "+errors);
                ByteBuffer code=shaderc_result_get_bytes(result);
                if(code==null || code.remaining()<20 || code.order(ByteOrder.LITTLE_ENDIAN).getInt(0)!=0x07230203)
                    throw new AssertionError("Invalid SPIR-V output");
                MessageDigest hash=MessageDigest.getInstance("SHA-256");hash.update(code);
                System.out.println("PASS "+name+" SHA256="+HexFormat.of().formatHex(hash.digest()));
            } else {
                if(status==shaderc_compilation_status_success || errors.isBlank() || !errors.contains(name))
                    throw new AssertionError("Missing invalid-shader diagnostic: "+errors);
                System.out.println("PASS invalid shader returns diagnostic");
            }
        } finally {shaderc_result_release(result);}
    }
    public static void main(String[] args) throws Exception {
        long compiler=shaderc_compiler_initialize(),options=shaderc_compile_options_initialize();
        if(compiler==0 || options==0)throw new AssertionError("Compiler initialization failed");
        try {
            // Match Minecraft 26.3 GlslCompiler's base settings, including its Vulkan 1.2 SPIR-V target even on OpenGL.
            shaderc_compile_options_set_target_env(options,shaderc_target_env_vulkan,shaderc_env_version_vulkan_1_2);
            shaderc_compile_options_set_auto_bind_uniforms(options,true);
            shaderc_compile_options_set_preserve_bindings(options,false);
            shaderc_compile_options_set_generate_debug_info(options);
            shaderc_compile_options_set_optimization_level(options,shaderc_optimization_level_zero);
            compile(compiler,options,"test.vert","#version 450\nlayout(location=0) in vec3 p; void main(){gl_Position=vec4(p,1);}",shaderc_vertex_shader,true);
            compile(compiler,options,"test.frag","#version 450\nlayout(location=0) out vec4 c; void main(){c=vec4(0.2,0.4,0.8,1);}",shaderc_fragment_shader,true);
            compile(compiler,options,"invalid.frag","#version 450\nvoid main(){ syntax error; }",shaderc_fragment_shader,false);
            if(args.length>0) {
                try(ZipFile jar=new ZipFile(args[0]);
                    ShadercIncludeResolve resolver=ShadercIncludeResolve.create((data,requested,type,requesting,depth)->{
                        String name=MemoryUtil.memUTF8(requested);
                        String path="assets/minecraft/shaders/include/"+name.replaceFirst("^minecraft:","");
                        String text;
                        try {
                            var entry=jar.getEntry(path);
                            if(entry==null)throw new IllegalArgumentException("Missing include "+name);
                            try(var in=jar.getInputStream(entry)){text=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
                        }catch(Exception e){text="#error "+e.getMessage()+"\n";}
                        return ShadercIncludeResult.calloc().source_name(MemoryUtil.memUTF8(name,false)).content(MemoryUtil.memUTF8(text,false)).address();
                    });
                    ShadercIncludeResultRelease release=ShadercIncludeResultRelease.create((data,address)->{
                        ShadercIncludeResult result=ShadercIncludeResult.create(address);
                        MemoryUtil.memFree(result.source_name());MemoryUtil.memFree(result.content());result.free();
                    })) {
                    shaderc_compile_options_set_include_callbacks(options,resolver,release,0);
                    for(String name:new String[]{"position_color.vsh","position_color.fsh"}) {
                        var entry=jar.getEntry("assets/minecraft/shaders/core/"+name);
                        String source;
                        try(var in=jar.getInputStream(entry)){source=new String(in.readAllBytes(),StandardCharsets.UTF_8);}
                        compile(compiler,options,name,source,name.endsWith("vsh")?shaderc_vertex_shader:shaderc_fragment_shader,true);
                    }
                    shaderc_compile_options_set_include_callbacks(options,null,null,0);
                }
            }
        } finally {shaderc_compile_options_release(options);shaderc_compiler_release(compiler);}
    }
}
