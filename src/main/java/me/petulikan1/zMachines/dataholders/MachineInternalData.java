package me.petulikan1.zMachines.dataholders;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class MachineInternalData {

    private long lastViewed;
    private transient long currentTick;


    public static MachineInternalData defaultData() {
        MachineInternalData data = new MachineInternalData();
        data.setLastViewed(0);
        data.setCurrentTick(0);
        return data;
    }

    public void incrementCurrentTick(){
        this.currentTick++;
    }

}
