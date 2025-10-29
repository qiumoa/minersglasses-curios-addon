package net.pathetic.minersglasses.curios.networking;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import net.pathetic.minersglasses.curios.MinersGlassesCuriosAddon;
import net.pathetic.minersglasses.curios.networking.packet.CuriosXrayC2SPacket;

/**
 * 网络消息管理类
 * 负责注册和管理客户端与服务端之间的网络通信
 */
public class ModMessages {
    /** 网络通道实例 */
    private static SimpleChannel INSTANCE;

    /** 数据包ID计数器 */
    private static int packetId = 0;

    /**
     * 获取下一个可用的数据包ID
     * @return 数据包ID
     */
    private static int id() {
        return packetId++;
    }

    /**
     * 注册网络消息通道和数据包
     * 在模组初始化时调用
     */
    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder
                .named(new ResourceLocation(MinersGlassesCuriosAddon.MOD_ID, "messages"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // 注册客户端到服务端的透视数据包
        net.messageBuilder(CuriosXrayC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER)
                .decoder(CuriosXrayC2SPacket::new)
                .encoder(CuriosXrayC2SPacket::toBytes)
                .consumerMainThread(CuriosXrayC2SPacket::handle)
                .add();
    }

    /**
     * 发送消息到服务端
     * @param message 要发送的消息对象
     * @param <MSG> 消息类型
     */
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }

    /**
     * 发送消息到指定玩家
     * @param message 要发送的消息对象
     * @param player 目标玩家
     * @param <MSG> 消息类型
     */
    public static <MSG> void sendToPlayer(MSG message, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), message);
    }
}
