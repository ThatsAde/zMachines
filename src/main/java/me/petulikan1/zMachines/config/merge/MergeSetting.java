package me.petulikan1.zMachines.config.merge;


import me.petulikan1.zMachines.config.Config;

public abstract class MergeSetting {
    public abstract boolean merge(Config config, Config merge);
}
