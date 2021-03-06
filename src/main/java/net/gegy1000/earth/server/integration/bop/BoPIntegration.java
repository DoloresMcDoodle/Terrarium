package net.gegy1000.earth.server.integration.bop;

import biomesoplenty.api.biome.BOPBiomes;
import net.gegy1000.earth.TerrariumEarth;
import net.gegy1000.earth.server.event.ConfigureFlowersEvent;
import net.gegy1000.earth.server.event.ConfigureTreesEvent;
import net.gegy1000.earth.server.event.InitBiomeClassifierEvent;
import net.gegy1000.earth.server.world.Climate;
import net.gegy1000.earth.server.world.EarthProperties;
import net.gegy1000.earth.server.world.composer.decoration.OreDecorationComposer;
import net.gegy1000.earth.server.world.cover.Cover;
import net.gegy1000.earth.server.world.cover.CoverMarkers;
import net.gegy1000.earth.server.world.cover.CoverSelectors;
import net.gegy1000.earth.server.world.ecology.GrowthPredictors;
import net.gegy1000.earth.server.world.ecology.soil.SoilSelector;
import net.gegy1000.earth.server.world.ecology.vegetation.FlowerDecorator;
import net.gegy1000.earth.server.world.ecology.vegetation.TreeDecorator;
import net.gegy1000.earth.server.world.ecology.vegetation.Trees;
import net.gegy1000.terrarium.server.capability.TerrariumWorld;
import net.gegy1000.terrarium.server.event.TerrariumInitializeGeneratorEvent;
import net.gegy1000.terrarium.server.world.generator.customization.GenerationSettings;
import net.minecraft.init.Biomes;
import net.minecraft.world.World;
import net.minecraft.world.biome.Biome;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;

public final class BoPIntegration {
    public static void setup() {
        MinecraftForge.TERRAIN_GEN_BUS.register(BoPIntegration.class);
        MinecraftForge.EVENT_BUS.register(BoPIntegration.class);
    }

    @SubscribeEvent
    public static void onConfigureTrees(ConfigureTreesEvent event) {
        TerrariumWorld terrarium = event.getTerrarium();
        if (!terrarium.getSettings().getBoolean(EarthProperties.BOP_INTEGRATION)) {
            return;
        }

        Cover cover = event.getCover();
        TreeDecorator.Builder trees = event.getBuilder();

        if (cover.is(CoverSelectors.broadleafDeciduous())) {
            trees.addCandidate(BoPTrees.MAHOGANY);
            trees.addCandidate(BoPTrees.WILLOW);
        }

        if (cover.is(CoverSelectors.broadleafEvergreen())) {
            trees.addCandidate(BoPTrees.PALM);
            trees.addCandidate(BoPTrees.EUCALYPTUS);
            trees.addCandidate(BoPTrees.MANGROVE);
            trees.addCandidate(BoPTrees.EBONY);
        }

        if (cover.is(CoverSelectors.needleleafEvergreen())) {
            trees.addCandidate(BoPTrees.FIR);
        }
    }

    @SubscribeEvent
    public static void onConfigureFlowers(ConfigureFlowersEvent event) {
        // TODO: disabling to only have flower generation in compatibility mode
        //  otherwise the integration mode would have to be fully featured which it is not yet
        if (true) return;

        TerrariumWorld terrarium = event.getTerrarium();
        if (!terrarium.getSettings().getBoolean(EarthProperties.BOP_INTEGRATION)) {
            return;
        }

        Cover cover = event.getCover();
        GrowthPredictors predictors = event.getPredictors();
        FlowerDecorator flowers = event.getFlowers();

        boolean hot = Climate.isHot(predictors.meanTemperature);
        boolean warm = Climate.isWarm(predictors.meanTemperature);
        boolean cold = Climate.isCold(predictors.meanTemperature);
        boolean frozen = Climate.isFrozen(predictors.minTemperature, predictors.meanTemperature);

        boolean wet = Climate.isWet(predictors.annualRainfall);

        if (cover.is(CoverMarkers.NEEDLELEAF) && cover.is(CoverMarkers.DECIDUOUS)) {
            flowers.add(BoPFlowers.LILY_OF_THE_VALLEY, 1.0F);
            flowers.add(BoPFlowers.CLOVER, 1.0F);
        }

        if (cover.is(CoverMarkers.PLAINS)) {
            flowers.add(BoPFlowers.CLOVER, 1.0F);
            flowers.add(BoPFlowers.LAVENDER, 0.1F);
            flowers.add(BoPFlowers.GOLDENROD, 0.1F);
        }

        if (cover.is(CoverMarkers.FLOODED)) {
            flowers.add(BoPFlowers.SWAMPFLOWER, 2.0F);
            flowers.add(BoPFlowers.BLUE_HYDRANGEA, 1.0F);
        }

        if (cover.is(CoverMarkers.CLOSED_FOREST)) {
            flowers.add(BoPFlowers.DEATHBLOOM, 0.1F);
        }

        if (cover.is(CoverMarkers.FOREST)) {
            flowers.add(BoPFlowers.GLOWFLOWER, 0.001F);
        }

        if (wet || cover.is(CoverMarkers.FLOODED)) {
            flowers.add(BoPFlowers.ORANGE_COSMOS, 1.0F);
            flowers.add(BoPFlowers.WHITE_ANEMONE, 1.0F);
            flowers.add(BoPFlowers.PINK_DAFFODIL, 1.0F);
            flowers.add(BoPFlowers.PINK_HIBISCUS, 1.0F);
        }

        if (hot) {
            flowers.add(BoPFlowers.WILDFLOWER, 2.0F);
        }

        if (warm && wet) {
            flowers.add(BoPFlowers.BROMELIAD, 3.0F);
        }

        if (cold) {
            flowers.add(BoPFlowers.VIOLET, 2.0F);
            flowers.add(BoPFlowers.BLUEBELLS, 2.0F);
        }

        if (frozen) {
            flowers.add(BoPFlowers.ICY_IRIS, 3.0F);
        }
    }

    @SubscribeEvent
    public static void onInitBiomeClassifier(InitBiomeClassifierEvent event) {
        GenerationSettings settings = event.getSettings();
        if (!settings.getBoolean(EarthProperties.BOP_INTEGRATION)) {
            return;
        }

        event.modifyClassifier(standard -> {
            return predictors -> {
                Biome biome = standard.classify(predictors);
                return classifyBiome(predictors, biome);
            };
        });
    }

    private static Biome classifyBiome(GrowthPredictors predictors, Biome biome) {
        if (biome == Biomes.BEACH) return biome;

        if (!predictors.isFrozen()) {
            if (predictors.cover == Cover.LICHENS_AND_MOSSES) {
                return BOPBiomes.tundra.or(biome);
            }

            if (predictors.cover == Cover.GRASSLAND) {
                return BOPBiomes.grassland.or(biome);
            }

            if (predictors.cover.is(CoverMarkers.DENSE_SHRUBS) && Climate.isVeryDry(predictors.annualRainfall)) {
                if (predictors.isBarren() || SoilSelector.isDesertLike(predictors)) {
                    return BOPBiomes.brushland.or(biome);
                } else {
                    return BOPBiomes.xeric_shrubland.or(biome);
                }
            }

            if (predictors.isLand() && !predictors.cover.is(CoverMarkers.NO_VEGETATION)) {
                double mangrove = BoPTrees.Indicators.MANGROVE.evaluate(predictors);
                if (mangrove > 0.85) {
                    return BOPBiomes.mangrove.or(biome);
                }
            }

            if (predictors.slope >= 60 && !predictors.isCold() && predictors.cover.is(CoverMarkers.FOREST)) {
                return BOPBiomes.overgrown_cliffs.or(biome);
            }

            if (predictors.isFlooded() && biome == Biomes.SWAMPLAND) {
                float spruce = Trees.Indicators.SPRUCE.evaluate(predictors);
                if (spruce > 0.85F) {
                    return BOPBiomes.wetland.or(biome);
                }
            }

            if (predictors.isForested() && biome == Biomes.JUNGLE) {
                float jungle = Trees.Indicators.JUNGLE_LIKE.evaluate(predictors);
                float oak = Trees.Indicators.OAK.evaluate(predictors);
                float spruce = Trees.Indicators.SPRUCE.evaluate(predictors);
                float mahogany = BoPTrees.Indicators.MAHOGANY.evaluate(predictors);

                if (oak > jungle && oak > spruce && oak > mahogany) {
                    return BOPBiomes.rainforest.or(biome);
                } else if (spruce > jungle && spruce > oak && spruce > mahogany) {
                    return BOPBiomes.temperate_rainforest.or(biome);
                } else if (mahogany > jungle && mahogany > spruce && mahogany > oak) {
                    return BOPBiomes.tropical_rainforest.or(biome);
                }
            }
        }

        if (predictors.isForested()) {
            return classifyForest(predictors, biome);
        }

        return biome;
    }

    private static Biome classifyForest(GrowthPredictors predictors, Biome biome) {
        if (biome == Biomes.FOREST && predictors.isFrozen()) {
            return BOPBiomes.snowy_forest.or(biome);
        }

        if (!predictors.isFrozen()) {
            float eucalyptus = BoPTrees.Indicators.EUCALYPTUS.evaluate(predictors);
            if (eucalyptus > 0.85F) {
                return BOPBiomes.eucalyptus_forest.or(biome);
            }

            float birch = Trees.Indicators.BIRCH.evaluate(predictors);
            float spruce = Trees.Indicators.SPRUCE.evaluate(predictors);
            if (birch > 0.85F && spruce > 0.85F) {
                return BOPBiomes.boreal_forest.or(biome);
            }
        }

        double fir = BoPTrees.Indicators.FIR.evaluate(predictors);
        if (fir > 0.85F) {
            if (predictors.isFrozen()) {
                return BOPBiomes.snowy_coniferous_forest.or(biome);
            } else {
                return BOPBiomes.coniferous_forest.or(biome);
            }
        }

        return biome;
    }

    @SubscribeEvent
    public static void onInitializeTerrariumGenerator(TerrariumInitializeGeneratorEvent event) {
        // TODO: disabling to only have ore generation in compatibility mode
        //  otherwise the integration mode would have to be fully featured which it is not yet
        if (true) return;

        if (event.getWorldType() != TerrariumEarth.GENERIC_WORLD_TYPE) return;

        World world = event.getWorld();
        GenerationSettings settings = event.getSettings();

        if (!settings.getBoolean(EarthProperties.BOP_INTEGRATION)) {
            return;
        }

        if (settings.getBoolean(EarthProperties.ORE_GENERATION)) {
            OreDecorationComposer oreComposer = new OreDecorationComposer(world);
            BoPOres.addTo(oreComposer);

            event.getGenerator().addDecorationComposer(oreComposer);
        }
    }
}
