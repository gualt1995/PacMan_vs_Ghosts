package genetic;

import io.jenetics.*;
import io.jenetics.engine.Engine;
import io.jenetics.engine.EvolutionResult;
import io.jenetics.util.Factory;
import io.jenetics.util.IntRange;
import pacman.Executor;
import pacman.controllers.IndividualGhostController;
import pacman.controllers.MASController;
import pacman.game.Constants;
import pacman.game.util.Stats;

import java.util.EnumMap;
import java.util.function.Function;

public class GAMain {

    public static void main(String... args) {
        Executor executor = new Executor.Builder()
                .setTickLimit(4000)
                .build();

        EnumMap<Constants.GHOST, IndividualGhostController> controllers = new EnumMap<>(Constants.GHOST.class);

        controllers.put(Constants.GHOST.INKY, new GAGhost(Constants.GHOST.INKY));
        controllers.put(Constants.GHOST.BLINKY, new GAGhost(Constants.GHOST.BLINKY));
        controllers.put(Constants.GHOST.PINKY, new GAGhost(Constants.GHOST.PINKY));
        controllers.put(Constants.GHOST.SUE, new GAGhost(Constants.GHOST.SUE));

        if (args.length > 0) {
            // [[2],[2],[2],[2],[3],[3],[2],[1],[3],[3],[2],[1],[2],[1],[2],[3]] -> best ?
            // [[1],[1],[1],[1],[1],[1],[3],[2],[3],[1],[3],[2],[1],[1],[3],[2]] -> ~1094
            String chromosome_str = "[[3],[2],[2],[2],[2],[1],[3],[2],[2],[3],[1],[3],[2],[2],[3],[1]]";
            String[] genes_str = chromosome_str.split(",");
            IntegerGene[] genes = new IntegerGene[16];
            IntRange range = IntRange.of(1, 3);
            int i = 0;
            for (String g : genes_str) {
                int value = Integer.parseInt(g.replaceAll("[\\[\\]]", ""));
                genes[i] = IntegerGene.of(value, range);
                i++;
            }

            IntegerChromosome ch = IntegerChromosome.of(genes);
            ((GAGhost)controllers.get(Constants.GHOST.INKY)).setChromosome(ch);
            ((GAGhost)controllers.get(Constants.GHOST.BLINKY)).setChromosome(ch);
            ((GAGhost)controllers.get(Constants.GHOST.PINKY)).setChromosome(ch);
            ((GAGhost)controllers.get(Constants.GHOST.SUE)).setChromosome(ch);

            Stats[] stats = executor.runExperiment(new examples.StarterPacMan.MyPacMan(), new MASController(controllers), 500, "GA");
            for (Stats s : stats) {
                System.out.println(s);
            }
            return;
        }


        // Init GA
        Factory<Genotype<IntegerGene>> gtf = Genotype.of(IntegerChromosome.of(1, 3, 16));

        Function<Genotype<IntegerGene>, Double> ff = gt -> {
            ((GAGhost)controllers.get(Constants.GHOST.INKY)).setChromosome(gt.getChromosome());
            ((GAGhost)controllers.get(Constants.GHOST.BLINKY)).setChromosome(gt.getChromosome());
            ((GAGhost)controllers.get(Constants.GHOST.PINKY)).setChromosome(gt.getChromosome());
            ((GAGhost)controllers.get(Constants.GHOST.SUE)).setChromosome(gt.getChromosome());
            Stats[] stats = executor.runExperiment(new examples.StarterPacMan.MyPacMan(),
                    new MASController(controllers), 5, "GA");
            return -stats[0].getAverage();
        };

        Engine<IntegerGene, Double> engine = Engine
                .builder(ff, gtf)
                .offspringFraction(0.7)
                .survivorsSelector(new RouletteWheelSelector<>())
                .offspringSelector(new TournamentSelector<>())
//                .alterers(new SinglePointCrossover<>(0.5), new Mutator<>(0.1))
                .alterers(new MultiPointCrossover<>(), new GaussianMutator<>())
                .build();

        Genotype<IntegerGene> result = engine.stream()
                .limit(100)
                .collect(EvolutionResult.toBestGenotype());

        System.out.println(result.getChromosome());

        ((GAGhost)controllers.get(Constants.GHOST.INKY)).setChromosome(result.getChromosome());
        ((GAGhost)controllers.get(Constants.GHOST.BLINKY)).setChromosome(result.getChromosome());
        ((GAGhost)controllers.get(Constants.GHOST.PINKY)).setChromosome(result.getChromosome());
        ((GAGhost)controllers.get(Constants.GHOST.SUE)).setChromosome(result.getChromosome());
        Stats[] stats = executor.runExperiment(new examples.StarterPacMan.MyPacMan(), new MASController(controllers), 100, "GA");
        for (Stats s : stats) {
            System.out.println(s);
        }
    }
}
