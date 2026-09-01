package xyz.iwolfking.woldsvaults.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import iskallia.vault.item.crystal.CrystalData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.items.ItemHandlerHelper;
import xyz.iwolfking.woldsvaults.init.ModConfigs;
import xyz.iwolfking.woldsvaults.init.ModItems;
import xyz.iwolfking.woldsvaults.items.ExtractorItem;
import xyz.iwolfking.woldsvaults.objectives.ExtractionCrystalObjective;

public class ExtractionCommand {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("wvextraction")
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("extractor").executes(context -> giveExtractor(context.getSource())))
                .then(Commands.literal("cells")
                        .executes(context -> giveCells(context.getSource(), 16))
                        .then(Commands.argument("count", IntegerArgumentType.integer(1, 1024))
                                .executes(context -> giveCells(context.getSource(), IntegerArgumentType.getInteger(context, "count")))))
                .then(Commands.literal("crystal")
                        .executes(context -> giveCrystal(context.getSource(), 100))
                        .then(Commands.argument("level", IntegerArgumentType.integer(0, 1000))
                                .executes(context -> giveCrystal(context.getSource(), IntegerArgumentType.getInteger(context, "level"))))));
    }

    private static int giveExtractor(CommandSourceStack source) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack stack = ExtractorItem.buildDefault(ModItems.EXTRACTOR, ModConfigs.EXTRACTION_OBJECTIVE);
        ItemHandlerHelper.giveItemToPlayer(player, stack);
        source.sendSuccess(new TextComponent("Gave a default extractor (stats from extraction_objective.json)."), false);
        return 1;
    }

    private static int giveCells(CommandSourceStack source, int count) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemHandlerHelper.giveItemToPlayer(player, new ItemStack(ModItems.POWER_CELL, count));
        source.sendSuccess(new TextComponent("Gave " + count + " power cells."), false);
        return 1;
    }

    private static int giveCrystal(CommandSourceStack source, int level) throws com.mojang.brigadier.exceptions.CommandSyntaxException {
        ServerPlayer player = source.getPlayerOrException();
        ItemStack crystal = new ItemStack(iskallia.vault.init.ModItems.VAULT_CRYSTAL);
        CrystalData data = CrystalData.read(crystal);
        data.getProperties().setLevel(level);
        data.setObjective(new ExtractionCrystalObjective());
        data.getProperties().setUnmodifiable(true);
        data.write(crystal);
        ItemHandlerHelper.giveItemToPlayer(player, crystal);
        source.sendSuccess(new TextComponent("Gave a level " + level + " extraction vault crystal."), false);
        return 1;
    }
}
