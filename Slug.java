// Simcenter STAR-CCM+ macro: Slug.java
// AoA sweep 0 → 180 deg at Mach 0.5
package macro;
import java.util.*;
import star.common.*;
import star.base.neo.*;
import star.flow.*;
public class Slug extends StarMacro {
  public void execute() { execute0(); }
  private void execute0() {
    Simulation sim = getActiveSimulation();
    Region region = sim.getRegionManager().getRegion("FV");
    Boundary farfield = region.getBoundaryManager().getBoundary("Far_Field");
    // --- Set Mach number once ---
    MachNumberProfile machProf = farfield.getValues().get(MachNumberProfile.class);
    Units unitless = (Units) sim.getUnitsManager().getObject(""); // dimensionless
    double mach = 1.25;
    machProf.getMethod(ConstantScalarProfileMethod.class).getQuantity().setValueAndUnits(mach, unitless);
    // --- Stopping criterion handle ---
    StepStoppingCriterion stopCrit =
      (StepStoppingCriterion) sim.getSolverStoppingCriterionManager().getSolverStoppingCriterion("Maximum Steps");
    IntegerValue maxSteps = stopCrit.getMaximumNumberStepsObject();
    // --- Force initial stopping criterion ---
    maxSteps.getQuantity().setValue(2000);
    // --- Sweep settings ---
    int stepAoA = 4;       // deg
    int maxAoA  = 180;     // deg
    int startIter = 2000;  // 2000 for production
    int addIter   = 1000;     // use 500  for production
    // get current iteration count when macro starts
    int baseIter = sim.getSimulationIterator().getCurrentIteration();
    int currentMax = 0;    // keep only the last two .sim files
    List<String> savedSims = new ArrayList<>();
    for (int aoa = 0; aoa <= maxAoA; aoa += stepAoA) {
      // set flow direction (cosθ, sinθ, 0)
      double rad = Math.toRadians(aoa);
      double x = Math.cos(rad), y = Math.sin(rad), z = 0.0;
      FlowDirectionProfile flowDir = farfield.getValues().get(FlowDirectionProfile.class);
      flowDir.getMethod(ConstantVectorProfileMethod.class).getQuantity().setComponentsAndUnits(x, y, z, unitless);
      // update cumulative max steps
      if (aoa == 0) currentMax = startIter; else currentMax += addIter;
      maxSteps.getQuantity().setValue(currentMax);
      // run to the new max step
      sim.getSimulationIterator().run();
      // save state to create a matching .out, then keep only the last two .sim
      String runDir = sim.getSessionDir();
      int machTag = (int)Math.round(mach * 100.0); // 0.50 -> 50
      String simName = String.format("Slug_M%03d_A%03d.sim", machTag, aoa);
      String simPath = runDir + "/" + simName;
      sim.saveState(simPath);              // writes .sim and matching .out
      savedSims.add(simPath);              // record this .sim
      if (savedSims.size() > 2) {          // if >2, delete the oldest .sim (keep its .out)
        String oldSim = savedSims.remove(0);
        java.io.File f = new java.io.File(oldSim);
        if (f.exists()) f.delete();
      }
      sim.println(String.format("AOA=%3d°, steps=%d, saved %s (keeping %d sims)",
                                aoa, currentMax, simName, savedSims.size()));
    }
  }
}