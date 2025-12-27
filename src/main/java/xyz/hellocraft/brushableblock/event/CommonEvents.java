package xyz.hellocraft.brushableblock.event;

import cy.jdkdigital.jearchaeology.JEArchaeology;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import xyz.hellocraft.brushableblock.BrushableBlock;
import xyz.hellocraft.brushableblock.block.BrushingManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
//import static xyz.hellocraft.brushableblock.BrushableBlock.LOGGER;

@EventBusSubscriber(modid = BrushableBlock.MODID)
public class CommonEvents {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        BrushingManager.tick(event.getLevel());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDataSync(OnDatapackSyncEvent event) {

        if (ModList.get().isLoaded(JEArchaeology.MODID)) {

            var player = event.getRelevantPlayers().findFirst();
            if (player.isPresent() && player.get().getServer() != null) {
                var recipeManager = player.get().getServer().getRecipeManager();

//            var brushRecipes = recipeManager.getAllRecipesFor(BRUSH_TYPE.get());

//            LOGGER.info("Find {} brushing recipes to player {}", brushRecipes.size(), player.get().getName().getString());
                Collection<RecipeHolder<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());
//              allRecipes.addAll(brushRecipes);
                List<RecipeHolder<?>> brushingRecipes = BrushingManager.getAllBrushingRecipes(event.getPlayerList().getServer().getLevel(Level.OVERWORLD));
                allRecipes.addAll(brushingRecipes);
                recipeManager.replaceRecipes(allRecipes);
                BrushableBlock.LOGGER.debug("JEArchaeology loaded, inject {} recipes for player {}", brushingRecipes.size(), player.get().getName().getString());

//            var newBrushRecipes = recipeManager.getAllRecipesFor(BRUSH_TYPE.get());
//            LOGGER.info("After sync, player {} has {} brushing recipes", player.get().getName().getString(), newBrushRecipes.size());

            }
        }
    }
}
