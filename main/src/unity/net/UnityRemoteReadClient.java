package unity.net;

import arc.*;
import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import mindustry.entities.*;
import mindustry.gen.*;
import mindustry.io.*;
import unity.*;
import unity.ai.KamiAI.*;

import java.io.*;

public class UnityRemoteReadClient{
    private static final ReusableByteInStream in = new ReusableByteInStream();
    private static final Reads read = new Reads(new DataInputStream(in));

    private static final IntMap<Runnable> map = new IntMap<>();

    public static void registerHandlers(){
        map.put(0, () -> {
            Bullet b = TypeIO.readBulletType(read).create(
                TypeIO.readEntity(read),
                TypeIO.readTeam(read),
                read.f(), read.f(), read.f()
            );
            b.vel.scl(read.f());
            b.lifetime = b.type.lifetime * read.f();

            float h = read.f();
            b.hitSize = h < 0f ? b.type.hitSize : h;
            b.data = KamiBulletDatas.get(read.i());
            b.fdata = read.f();
            b.time = read.f();

            Unity.print(b.owner, ((Unitc)b.owner).controller());
        });

        map.put(1, () -> {
            String name = read.str();
            boolean play = read.bool();

            if(play){
                Unity.musicHandler.play(name);
            }else{
                Unity.musicHandler.stop(name);
            }
        });

        map.put(2, () -> {
            Player player = TypeIO.readEntity(read);
            float x = read.f();
            float y = read.f();

            Unity.tapHandler.tap(player, x, y);
        });

        map.put(3, () -> {
            Effect effect = TypeIO.readEffect(read);
            float x = read.f();
            float y = read.f();
            float rotation = read.f();
            Object data;

            try{
                data = TypeIO.readObject(read);
            }catch(IllegalArgumentException no){
                byte type = read.b();
                switch(type){
                    case 16: {
                        Position[] pos = new Position[read.b()];
                        for(int i = 0; i < pos.length; i++){
                            pos[i] = new Vec2(read.f(), read.f());
                        }

                        data = pos;
                    }

                    case 17: {
                        data = Core.atlas.find(TypeIO.readString(read));
                    }

                    default: throw new IllegalArgumentException("Unknown object type: " + type);
                }
            }

            effect.at(x, y, rotation, data);
        });
    }

    public static void readPacket(byte[] bytes, byte type){
        in.setBytes(bytes);
        if(!map.containsKey(type)){
            throw new RuntimeException("Unknown packet type: '" + type + "'");
        }else{
            try{
                map.get(type).run();
            }catch(Exception e){
                throw new RuntimeException(e);
            }
        }
    }
}
