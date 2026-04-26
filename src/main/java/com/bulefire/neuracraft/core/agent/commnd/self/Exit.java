package com.bulefire.neuracraft.core.agent.commnd.self;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.function.process.PlayerExitEventProcesser;
import com.bulefire.neuracraft.compatibility.util.CUtil;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.PlayerManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Log4j2
public class Exit extends FullCommand.AbsCommand {
    @Override
    public int run(@NotNull CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        String playerName = Objects.requireNonNull(commandContext.getSource().getPlayer()).getName().getString();
        UUID playerUUID = Objects.requireNonNull(commandContext.getSource().getPlayer()).getUUID();
        var agentManager = AgentController.getInstance().getAgentManager();
        var playerManager = AgentController.getInstance().getPlayerManager();
        var player = new APlayer(playerName, playerUUID);
        var agentUUID = playerManager.getPlayerAgentUUID(player);
        if (agentUUID == null) {
            feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.exit.notInChatRoom"));
            return 1;
        }
        var agent = agentManager.getAgent(agentUUID);
        PlayerExitEventProcesser.onPlayerExit(new PlayerExitEventProcesser.ExitMessage(player, CUtil.getEnv(CUtil.getServer.get())));
        playerManager.updatePlayer(player, PlayerManager.emptyUUID);
        agent.removePlayer(player);
        agent.removeAdmin(player);
        feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.exit.success", agent.getName()));
        log.info("player {} exit agent: {}, UUID: {}", playerName, agent.getName(), agent.getUUID());
        return 1;
    }
}
