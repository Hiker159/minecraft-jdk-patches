/* SPDX-License-Identifier: GPL-2.0-only */
import java.nio.*;
import org.lwjgl.openal.*;
import org.lwjgl.system.MemoryUtil;
import static org.lwjgl.openal.AL10.*;
import static org.lwjgl.openal.ALC10.*;
import static org.lwjgl.openal.SOFTLoopback.*;
public class OpenALSmoke {
    static void check(boolean ok, String message) {
        if (!ok) throw new AssertionError(message);
    }
    public static void main(String[] args) throws Exception {
        boolean loopback=args.length>0 && args[0].equals("--loopback");
        long device=loopback ? alcLoopbackOpenDeviceSOFT((ByteBuffer)null) : alcOpenDevice((ByteBuffer)null);
        check(device!=0, "Open default audio device");
        long context=0;
        int buffer=0, source=0;
        try {
            ALCCapabilities dc=ALC.createCapabilities(device);
            check(dc.OpenALC11, "OpenALC 1.1 required by Minecraft");
            context=loopback ? alcCreateContext(device, new int[]{ALC_FORMAT_CHANNELS_SOFT, ALC_STEREO_SOFT,
                ALC_FORMAT_TYPE_SOFT, ALC_SHORT_SOFT, ALC_FREQUENCY, 44100, 0}) : alcCreateContext(device,(IntBuffer)null);
            check(context!=0 && alcMakeContextCurrent(context), "Create current audio context");
            ALCapabilities ac=AL.createCapabilities(dc);
            check(ac.AL_EXT_source_distance_model, "Minecraft source distance model");
            check(ac.AL_EXT_LINEAR_DISTANCE, "Minecraft linear distance");
            System.out.println("OpenAL: "+alGetString(AL_VERSION)+" / "+alGetString(AL_RENDERER));
            System.out.println("Device: "+alcGetString(device,ALC_DEVICE_SPECIFIER));
            System.out.println("HRTF="+dc.ALC_SOFT_HRTF+", disconnect="+alcIsExtensionPresent(device,"ALC_EXT_disconnect"));
            buffer=alGenBuffers();source=alGenSources();
            ShortBuffer silence=MemoryUtil.memCallocShort(4410);
            try {
                if(loopback) for(int i=0;i<silence.capacity();i++) silence.put(i,(short)(10000*Math.sin(i*2*Math.PI*440/44100)));
                alBufferData(buffer,AL_FORMAT_MONO16,silence,44100);
            }
            finally { MemoryUtil.memFree(silence); }
            alSourcei(source,AL_BUFFER,buffer);alSourcePlay(source);
            check(alGetError()==AL_NO_ERROR,"Upload and play buffer");
            if(loopback) {
                ShortBuffer mixed=MemoryUtil.memAllocShort(9600);
                try {
                    alcRenderSamplesSOFT(device,mixed,4800);
                    check(alcGetError(device)==ALC_NO_ERROR,"Render mixed audio");
                    boolean nonzero=false;
                    for(int i=0;i<mixed.capacity();i++) if(mixed.get(i)!=0) nonzero=true;
                    check(nonzero,"Synthesized tone produces audio samples");
                } finally { MemoryUtil.memFree(mixed); }
            }
            long deadline=System.nanoTime()+3_000_000_000L;
            while (alGetSourcei(source,AL_SOURCE_STATE)!=AL_STOPPED && System.nanoTime()<deadline) Thread.sleep(10);
            check(alGetSourcei(source,AL_SOURCE_STATE)==AL_STOPPED,"Audio playback completes");
            System.out.println("PASS: Minecraft-required extensions, device, context, buffer playback and cleanup path" + (loopback ? ", nonzero loopback mixing" : ""));
        } finally {
            if(source!=0)alDeleteSources(source);
            if(buffer!=0)alDeleteBuffers(buffer);
            alcMakeContextCurrent(0);
            if(context!=0)alcDestroyContext(context);
            check(alcCloseDevice(device),"Close audio device");
        }
    }
}
