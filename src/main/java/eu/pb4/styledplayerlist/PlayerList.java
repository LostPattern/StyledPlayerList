package eu.pb4.styledplayerlist;

import eu.pb4.placeholders.api.PlaceholderContext;
import eu.pb4.placeholders.api.Placeholders;
import eu.pb4.styledplayerlist.access.PlayerListViewerHolder;
import eu.pb4.styledplayerlist.command.Commands;
import eu.pb4.styledplayerlist.config.ConfigManager;
import eu.pb4.styledplayerlist.config.PlayerListStyle;
import eu.pb4.styledplayerlist.config.data.ConfigData;
import net.minecraft.network.packet.s2c.play.PlayerListHeaderS2CPacket;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardCriterion;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.LinkedHashMap;

@Mod("styledplayerlist")
@EventBusSubscriber
public class PlayerList  {
	public static final Logger LOGGER = LogManager.getLogger("Styled Player List");
	public static final String ID = "styledplayerlist";
	public static final Scoreboard SCOREBOARD = new Scoreboard();
	public static final String OBJECTIVE_NAME = "■SPL_OBJ";

	public static final ScoreboardObjective SCOREBOARD_OBJECTIVE = new ScoreboardObjective(
			SCOREBOARD, OBJECTIVE_NAME, ScoreboardCriterion.DUMMY,
			Text.empty(), ScoreboardCriterion.RenderType.INTEGER, false, null);


	public PlayerList(ModContainer container) {
		GenericModInfo.build(container);
		Placeholders.registerChangeEvent((a, b) -> ConfigManager.rebuildStyled());
	}


	@SubscribeEvent
	public static void onServerStarted(ServerStartedEvent event) {
		ConfigManager.loadConfig();
	}

	private void tick(MinecraftServer server) {
		if (ConfigManager.isEnabled()) {
			ConfigData config = ConfigManager.getConfig().configData;
			for (var player : server.getPlayerManager().getPlayerList()) {
				var x = System.nanoTime();
				if (!SPLHelper.shouldSendPlayerList(player) || player.networkHandler == null) {
					continue;
				}
				var tick = server.getTicks();
				var holder = (PlayerListViewerHolder) player.networkHandler;

				var style = holder.styledPlayerList$getStyleObject();

				if (tick % style.updateRate == 0) {
					var context = PlaceholderContext.of(player, SPLHelper.PLAYER_LIST_VIEW);
					var animationTick = holder.styledPlayerList$getAndIncreaseAnimationTick();
					player.networkHandler.send(new PlayerListHeaderS2CPacket(style.getHeader(context, animationTick), style.getFooter(context, animationTick)));
				}

				if (config.playerName.playerNameUpdateRate > 0 && tick % config.playerName.playerNameUpdateRate == 0) {
					holder.styledPlayerList$updateName();
				}
				player.sendMessage(Text.literal(tick + " | " + ((System.nanoTime() - x) / 1000000f)), true);
			}
		}
	}

	public static Identifier id(String path) {
		return Identifier.of(ID, path);
	}

	@FunctionalInterface
	public interface PlayerListStyleLoad {
		void onPlayerListUpdate(StyleHelper styleHelper);
	}


	public record StyleHelper(LinkedHashMap<String, PlayerListStyle> styles) {
		public void addStyle(PlayerListStyle style) {
			this.styles.put(style.id, style);
		}

		public void removeStyle(PlayerListStyle style) {
			this.styles.remove(style.id, style);
		}
	}


	public static String getPlayersStyle(ServerPlayerEntity player) {
		return ((PlayerListViewerHolder) player.networkHandler).styledPlayerList$getStyle();
	}

	public static void setPlayersStyle(ServerPlayerEntity player, String key) {
		((PlayerListViewerHolder) player).styledPlayerList$setStyle(key);
	}

	public static void addUpdateSkipCheck(ModCompatibility check) {
		SPLHelper.COMPATIBILITY.add(check);
	}

	public interface ModCompatibility {
		boolean check(ServerPlayerEntity player);
	}
}
