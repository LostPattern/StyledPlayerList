package eu.pb4.styledplayerlist;

import eu.pb4.placeholders.api.PlaceholderContext;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;

public class SPLHelper {
    public static final PlaceholderContext.ViewObject PLAYER_LIST_VIEW = PlaceholderContext.ViewObject.of(Identifier.of("styled_player_list", "player_list"));
    public static final PlaceholderContext.ViewObject PLAYER_NAME_VIEW = PlaceholderContext.ViewObject.of(Identifier.of("styled_player_list", "player_name"));
    public static Set<PlayerList.ModCompatibility> COMPATIBILITY = new HashSet<>();

    private static final Set<ServerPlayerEntity> BLOCKED_LAST_TIME = new HashSet<>();

    static {

    }

    public static boolean shouldSendPlayerList(ServerPlayerEntity player) {
        for (PlayerList.ModCompatibility mod : COMPATIBILITY) {
            boolean value = mod.check(player);

            if (value) {
                return false;
            }
        }
        return true;
    }
}
