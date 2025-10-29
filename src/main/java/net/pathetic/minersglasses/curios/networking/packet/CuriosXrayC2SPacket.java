package net.pathetic.minersglasses.curios.networking.packet;

import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.network.NetworkEvent;
import net.pathetic.minersglasses.curios.event.CuriosServerEvents;
import net.pathetic.minersglasses.curios.util.CuriosHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

/**
 * 客户端到服务端的透视数据包
 * 处理来自饰品槽的矿工眼镜透视功能
 */
public class CuriosXrayC2SPacket {
    private static final Logger LOGGER = LogManager.getLogger();

    public CuriosXrayC2SPacket() {
    }

    public CuriosXrayC2SPacket(FriendlyByteBuf buf) {
        // 空数据包，不需要读取数据
    }

    public void toBytes(FriendlyByteBuf buf) {
        // 空数据包，不需要写入数据
    }

    /**
     * 生成透视显示实体
     */
    private void summonXrayDisplays(ServerLevel level, BlockPos pos, String teamName,
                                    ChatFormatting teamColor, String tag) {
        // 创建或获取团队
        if (!level.getScoreboard().getPlayerTeams().contains(level.getScoreboard().getPlayerTeam(teamName))) {
            level.getScoreboard().addPlayerTeam(teamName);
            level.getScoreboard().getPlayerTeam(teamName).setColor(teamColor);
        }

        Commands commands = level.getServer().getCommands();
        // 创建静默命令源 - 不在聊天框显示命令输出
        CommandSourceStack source = level.getServer().createCommandSourceStack()
            .withSuppressedOutput();

        // 根据不同的维度执行命令
        String dimension;
        if (level.dimension() == ServerLevel.OVERWORLD) {
            dimension = "minecraft:overworld";
        } else if (level.dimension() == ServerLevel.NETHER) {
            dimension = "minecraft:the_nether";
        } else if (level.dimension() == ServerLevel.END) {
            dimension = "minecraft:the_end";
        } else {
            dimension = null;
        }

        double x = pos.getX() - 0.001;
        double y = pos.getY();
        double z = pos.getZ() - 0.001;

        String summonCommand;
        if (dimension != null) {
            summonCommand = String.format(
                "/execute in %s run summon minecraft:block_display %.3f %.1f %.3f {block_state:{Name:\"minersglasses:outline_xray_block\"},Glowing:1,Tags:[\"%s\"]}",
                dimension, x, y, z, tag
            );
        } else {
            summonCommand = String.format(
                "/summon minecraft:block_display %.3f %.1f %.3f {block_state:{Name:\"minersglasses:outline_xray_block\"},Glowing:1,Tags:[\"%s\"]}",
                x, y, z, tag
            );
        }

        commands.performPrefixedCommand(source, summonCommand);
        commands.performPrefixedCommand(source, "/team join " + teamName + " @e[tag=" + tag + "]");
    }

    /**
     * 获取眼镜类型对应的物品ID字符串
     */
    private String getGlassesType(ItemStack glasses) {
        String itemId = glasses.getItem().toString();
        if (itemId.contains("coal_glasses")) return "coal";
        if (itemId.contains("lapis_glasses")) return "lapis";
        if (itemId.contains("copper_glasses")) return "copper";
        if (itemId.contains("iron_glasses")) return "iron";
        if (itemId.contains("redstone_glasses")) return "redstone";
        if (itemId.contains("gold_glasses")) return "gold";
        if (itemId.contains("emerald_glasses")) return "emerald";
        if (itemId.contains("diamond_glasses")) return "diamond";
        if (itemId.contains("netherite_glasses")) return "netherite";
        return "basic";
    }

    /**
     * 处理数据包
     */
    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                LOGGER.warn("收到透视请求但发送者为空");
                return;
            }

            // 使用 CuriosHelper 获取装备的眼镜
            ItemStack glasses = CuriosHelper.getEquippedGlasses(player);

            if (glasses.isEmpty()) {
                LOGGER.debug("玩家 {} 未装备眼镜", player.getName().getString());
                return; // 没有装备眼镜
            }

            // 检查冷却状态（优先检查，因为这是最轻量的检查）
            if (player.getCooldowns().isOnCooldown(glasses.getItem())) {
                LOGGER.debug("玩家 {} 的眼镜还在冷却中", player.getName().getString());
                return; // 在冷却中，不执行透视
            }

            // 检查耐久度
            if (glasses.getMaxDamage() - glasses.getDamageValue() <= 1) {
                LOGGER.debug("玩家 {} 的眼镜耐久度不足", player.getName().getString());
                return; // 耐久度不足
            }

            ServerLevel level = player.serverLevel();
            BlockPos playerPos = player.blockPosition();
            String glassesType = getGlassesType(glasses);

            LOGGER.info("玩家 {} 使用 {} 眼镜触发透视扫描，位置: {}",
                player.getName().getString(), glassesType, playerPos);

            // 执行透视扫描（扫描范围：32x32x20）
            performXrayScan(player, level, playerPos, glassesType);

            // 注册透视状态（用于自动清除）
            CuriosServerEvents.registerXray(player, glassesType);

            // 设置冷却时间和消耗耐久
            int cooldown = getCooldownForGlasses(glassesType);
            player.getCooldowns().addCooldown(glasses.getItem(), cooldown);
            glasses.hurtAndBreak(1, player, (p) -> {
                p.broadcastBreakEvent(glasses.getEquipmentSlot());
            });

            LOGGER.debug("透视扫描完成，冷却时间: {} ticks, 持续时间: {} ticks",
                cooldown, getDurationForGlasses(glassesType));
        });

        return true;
    }

    /**
     * 执行透视扫描
     */
    private void performXrayScan(ServerPlayer player, ServerLevel level, BlockPos playerPos, String glassesType) {
        // 扫描范围：玩家位置为中心，X和Z轴各±16，Y轴-10到+10
        for (int x = playerPos.getX(); x <= playerPos.getX() + 32; x++) {
            for (int z = playerPos.getZ(); z <= playerPos.getZ() + 32; z++) {
                for (int y = playerPos.getY(); y <= playerPos.getY() + 20; y++) {
                    BlockPos pos = BlockPos.containing(x - 16, y - 10, z - 16);
                    BlockState state = level.getBlockState(pos);

                    // 根据眼镜类型检测不同的矿石
                    checkAndDisplayOre(level, pos, state, glassesType);
                }
            }
        }
    }

    /**
     * 检查并显示矿石
     */
    private void checkAndDisplayOre(ServerLevel level, BlockPos pos, BlockState state, String glassesType) {
        // 煤炭（所有眼镜都能看到）
        if (state.is(Blocks.COAL_ORE) || state.is(Blocks.DEEPSLATE_COAL_ORE)) {
            summonXrayDisplays(level, pos, "xray_coal", ChatFormatting.DARK_GRAY, "coal_xray");
        }

        // 青金石（lapis及以上）
        if (!glassesType.equals("coal") && !glassesType.equals("basic")) {
            if (state.is(Blocks.LAPIS_ORE) || state.is(Blocks.DEEPSLATE_LAPIS_ORE)) {
                summonXrayDisplays(level, pos, "lapis_xray", ChatFormatting.BLUE, "lapis_xray");
            }
        }

        // 铜矿（copper及以上）
        if (glassesType.equals("copper") || glassesType.equals("iron") || glassesType.equals("redstone") ||
            glassesType.equals("gold") || glassesType.equals("emerald") || glassesType.equals("diamond") ||
            glassesType.equals("netherite")) {
            if (state.is(Blocks.COPPER_ORE) || state.is(Blocks.DEEPSLATE_COPPER_ORE)) {
                summonXrayDisplays(level, pos, "xray_copper", ChatFormatting.GOLD, "copper_xray");
            }
        }

        // 铁矿（iron及以上）
        if (glassesType.equals("iron") || glassesType.equals("redstone") || glassesType.equals("gold") ||
            glassesType.equals("emerald") || glassesType.equals("diamond") || glassesType.equals("netherite")) {
            if (state.is(Blocks.IRON_ORE) || state.is(Blocks.DEEPSLATE_IRON_ORE)) {
                summonXrayDisplays(level, pos, "xray_iron", ChatFormatting.GRAY, "iron_xray");
            }
        }

        // 红石（redstone及以上）
        if (glassesType.equals("redstone") || glassesType.equals("gold") || glassesType.equals("emerald") ||
            glassesType.equals("diamond") || glassesType.equals("netherite")) {
            if (state.is(Blocks.REDSTONE_ORE) || state.is(Blocks.DEEPSLATE_REDSTONE_ORE)) {
                summonXrayDisplays(level, pos, "xray_redstone", ChatFormatting.RED, "redstone_xray");
            }
        }

        // 金矿（gold及以上）
        if (glassesType.equals("gold") || glassesType.equals("emerald") || glassesType.equals("diamond") ||
            glassesType.equals("netherite")) {
            if (state.is(Blocks.GOLD_ORE) || state.is(Blocks.DEEPSLATE_GOLD_ORE)) {
                summonXrayDisplays(level, pos, "xray_gold", ChatFormatting.YELLOW, "gold_xray");
            }
        }

        // 绿宝石（emerald及以上）
        if (glassesType.equals("emerald") || glassesType.equals("diamond") || glassesType.equals("netherite")) {
            if (state.is(Blocks.EMERALD_ORE) || state.is(Blocks.DEEPSLATE_EMERALD_ORE)) {
                summonXrayDisplays(level, pos, "xray_emerald", ChatFormatting.GREEN, "emerald_xray");
            }
        }

        // 钻石（diamond及以上）
        if (glassesType.equals("diamond") || glassesType.equals("netherite")) {
            if (state.is(Blocks.DIAMOND_ORE) || state.is(Blocks.DEEPSLATE_DIAMOND_ORE)) {
                summonXrayDisplays(level, pos, "xray_diamond", ChatFormatting.AQUA, "diamond_xray");
            }
        }

        // 远古残骸（仅netherite）
        if (glassesType.equals("netherite")) {
            if (state.is(Blocks.ANCIENT_DEBRIS)) {
                summonXrayDisplays(level, pos, "xray_netherite", ChatFormatting.BLACK, "netherite_xray");
            }
        }
    }

    /**
     * 获取不同眼镜的冷却时间
     */
    private int getCooldownForGlasses(String glassesType) {
        switch (glassesType) {
            case "coal":
            case "lapis":
            case "copper":
                return 300;
            case "iron":
            case "redstone":
            case "gold":
                return 200;
            case "emerald":
            case "diamond":
                return 160;
            case "netherite":
                return 140;
            default:
                return 300;
        }
    }

    /**
     * 获取不同眼镜的透视持续时间（独立于冷却时间）
     * 设计理念：越高级的眼镜，持续时间越长
     */
    public static int getDurationForGlasses(String glassesType) {
        switch (glassesType) {
            case "coal":
                return 100;  // 5秒
            case "lapis":
                return 120;  // 6秒
            case "copper":
                return 140;  // 7秒
            case "iron":
                return 160;  // 8秒
            case "redstone":
                return 180;  // 9秒
            case "gold":
                return 200;  // 10秒
            case "emerald":
                return 220;  // 11秒
            case "diamond":
                return 240;  // 12秒
            case "netherite":
                return 300;  // 15秒
            default:
                return 100;
        }
    }
}
