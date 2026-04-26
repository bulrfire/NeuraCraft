package com.bulefire.neuracraft.core.agent.commnd.self;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.function.process.PlayerJoinEventProcesser;
import com.bulefire.neuracraft.compatibility.util.CUtil;
import com.bulefire.neuracraft.core.agent.Agent;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import lombok.extern.log4j.Log4j2;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.UUID;

@Log4j2
public class Join extends FullCommand.AbsCommand {
    @Override
    public int run(@NotNull CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        new Exit().run(commandContext);
        String playerName = Objects.requireNonNull(commandContext.getSource().getPlayer()).getName().getString();
        UUID playerUUID = Objects.requireNonNull(commandContext.getSource().getPlayer()).getUUID();
        var agentManager = AgentController.getInstance().getAgentManager();
        var playerManager = AgentController.getInstance().getPlayerManager();
        var player = new APlayer(playerName, playerUUID);
        Agent agent;
        String agentName;
        String agentUUID;
        try {
            agentName = StringArgumentType.getString(commandContext, "agentName");
        } catch (IllegalArgumentException e) {
            agentName = null;
        }
        try {
            agentUUID = StringArgumentType.getString(commandContext, "agentUUID");
        } catch (IllegalArgumentException e) {
            agentUUID = null;
        }

        if (agentName != null && agentUUID == null) {
            var agents = agentManager.getAgentByName(agentName);
            if (agents.size() != 1) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.join.failure.manyAgents", agents.size(), agentName));
                return 1;
            }
            agent = agents.get(0);
        } else if (agentName == null && agentUUID != null) {
            agent = agentManager.getAgent(UUID.fromString(agentUUID));
            if (agent == null) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.join.failure.notExist", agentUUID));
                return 1;
            }
        } else {
            return 0;
        }

        agent.addPlayer(player);
        playerManager.updatePlayer(player, agent.getUUID());

        feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.join.success", agent.getName()));
        PlayerJoinEventProcesser.onPlayerJoin(new PlayerJoinEventProcesser.JoinMessage(player, CUtil.getEnv(CUtil.getServer.get())));
        log.info("player {} join agent: {}, UUID: {}", playerName, agent.getName(), agent.getUUID());
        return 1;
    }
}
