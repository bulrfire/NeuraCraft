package com.bulefire.neuracraft.core.agent.commnd.control;

import com.bulefire.neuracraft.compatibility.command.FullCommand;
import com.bulefire.neuracraft.compatibility.entity.APlayer;
import com.bulefire.neuracraft.compatibility.util.FileUtil;
import com.bulefire.neuracraft.core.agent.Agent;
import com.bulefire.neuracraft.core.agent.AgentController;
import com.bulefire.neuracraft.core.agent.PlayerManager;
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
public class Delete extends FullCommand.AbsCommand {
    @Override
    public int run(@NotNull CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
        String playerName = Objects.requireNonNull(commandContext.getSource().getPlayer()).getName().getString();
        UUID playerUUID = commandContext.getSource().getPlayer().getUUID();
        var player = new APlayer(playerName, playerUUID);
        var agentManager = AgentController.getInstance().getAgentManager();
        var playerManager = AgentController.getInstance().getPlayerManager();
        String agentName;
        UUID agentUUID;
        Agent agent;
        try {
            agentName = StringArgumentType.getString(commandContext, "agentName");
        } catch (IllegalArgumentException e) {
            agentName = null;
        }
        try {
            agentUUID = UUID.fromString(StringArgumentType.getString(commandContext, "agentUUID"));
        } catch (IllegalArgumentException e) {
            agentUUID = null;
        }
        if (agentName != null && agentUUID == null) {
            var agents = agentManager.getAgentByName(agentName);
            if (agents.size() != 1) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.manyAgents", agents.size(), agentName));
                return 1;
            }
            agent = agents.get(0);
        } else if (agentName == null && agentUUID != null) {
            agent = agentManager.getAgent(agentUUID);
            if (agent == null) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.noAgent", agentUUID));
                return 1;
            }
        } else if (agentName == null && agentUUID == null) {
            agent = agentManager.getAgent(playerManager.getPlayerAgentUUID(player));
            if (agent == null) {
                feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.playerNotInAgent"));
                return 1;
            }
        } else {
            return 0;
        }

        agentUUID = agent.getUUID();
        if (! agent.getAdmins().contains(player) && agent.hasAdmin(player) && ! commandContext.getSource().hasPermission(4)) {
            feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.notAdmin", agent.getName()));
            return 1;
        }
        agent.getPlayers().forEach(player1 -> playerManager.updatePlayer(player1, PlayerManager.emptyUUID));
        agentManager.removeAgent(agentUUID);

        FileUtil.deleteFile(AgentController.getAgentPath(agent));

        feedback(commandContext.getSource(), Component.translatable("neuracraft.agent.command.delete.success", agentName, agentUUID));
        log.info("player {} delete Agent: {} , UUID: {}", playerName, agentName, agentUUID);
        return 1;
    }
}
