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

import static cy.jdkdigital.jearchaeology.JEArchaeology.BRUSH_TYPE;
//import static xyz.hellocraft.brushableblock.BrushableBlock.LOGGER;

@EventBusSubscriber(modid = BrushableBlock.MODID)
public class CommonEvents {
//    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(BuiltInRegistries.RECIPE_SERIALIZER, "jearchaeology");
//    public static final DeferredRegister<RecipeType<?>> JEA_RECIPE_TYPES = DeferredRegister.create(Registries.RECIPE_TYPE, "jearchaeology");
//    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<BrushingRecipe>> BRUSH = RECIPE_SERIALIZERS.register("brush", BrushingRecipe.Serializer::new);
//    public static DeferredHolder<RecipeType<?>, RecipeType<BrushingRecipe>> BRUSH_TYPE = JEA_RECIPE_TYPES.register("brush", () -> new RecipeType<>() {});

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        BrushingManager.tick(event.getLevel());
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onDataSync(OnDatapackSyncEvent event) {

        if (!ModList.get().isLoaded(JEArchaeology.MODID)) {
            return;
        }

        var player = event.getRelevantPlayers().findFirst();
        if (player.isPresent() && player.get().getServer() != null) {
            var recipeManager = player.get().getServer().getRecipeManager();

//            var brushRecipes = recipeManager.getAllRecipesFor(BRUSH_TYPE.get());

//            LOGGER.info("Find {} brushing recipes to player {}", brushRecipes.size(), player.get().getName().getString());
            Collection<RecipeHolder<?>> allRecipes = new ArrayList<>(recipeManager.getRecipes());
//            allRecipes.addAll(brushRecipes);
            allRecipes.addAll(BrushingManager.getAllBrushingRecipes(event.getPlayerList().getServer().getLevel(Level.OVERWORLD)));
            recipeManager.replaceRecipes(allRecipes);

//            var newBrushRecipes = recipeManager.getAllRecipesFor(BRUSH_TYPE.get());
//            LOGGER.info("After sync, player {} has {} brushing recipes", player.get().getName().getString(), newBrushRecipes.size());

        }
    }
}
