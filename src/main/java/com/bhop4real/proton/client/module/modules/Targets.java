package com.bhop4real.proton.client.module.modules;

import com.bhop4real.proton.client.ProtonClient;
import com.bhop4real.proton.client.module.Module;
import com.bhop4real.proton.client.module.ModuleManager;
import com.bhop4real.proton.client.module.settings.BooleanSetting;
import com.bhop4real.proton.client.module.settings.SeparatorSetting;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.passive.EntityAmbientCreature;
import net.minecraft.entity.passive.EntitySquid;
import net.minecraft.entity.passive.EntityVillager;
import net.minecraft.entity.monster.EntityIronGolem;
import net.minecraft.entity.monster.EntitySnowman;
import net.minecraft.entity.player.EntityPlayer;

/**
 * Centralises target selection rules for combat modules.
 */
public class Targets extends Module
{
    private static Targets instance;

    private static final class Defaults
    {
        static final boolean PLAYERS = true;
        static final boolean MONSTERS = false;
        static final boolean ANIMALS = false;
        static final boolean VILLAGERS = false;
        static final boolean GOLEMS = false;
        static final boolean OTHERS = false;
        static final boolean INCLUDE_INVISIBLE = false;
        static final boolean INCLUDE_DEAD = false;

        private Defaults() {}
    }

    private final BooleanSetting players;
    private final BooleanSetting monsters;
    private final BooleanSetting animals;
    private final BooleanSetting villagers;
    private final BooleanSetting golems;
    private final BooleanSetting others;
    private final BooleanSetting includeInvisible;
    private final BooleanSetting includeDead;

    public Targets()
    {
        super("Targets", new String[]{"targeting"}, "Choose which entities count as valid targets", Module.Category.MISC, 0);
        instance = this;

        addSetting(new SeparatorSetting("Entity Types"));

        players = new BooleanSetting("Players", "Allow targeting other players", Defaults.PLAYERS);
        monsters = new BooleanSetting("Monsters", "Allow hostile mobs", Defaults.MONSTERS);
        animals = new BooleanSetting("Animals", "Allow passive animals and ambient mobs", Defaults.ANIMALS);
        villagers = new BooleanSetting("Villagers", "Allow villagers", Defaults.VILLAGERS);
        golems = new BooleanSetting("Golems", "Allow iron or snow golems", Defaults.GOLEMS);
        others = new BooleanSetting("Others", "Allow other living entities (e.g. armor stands)", Defaults.OTHERS);

        addSetting(players);
        addSetting(monsters);
        addSetting(animals);
        addSetting(villagers);
        addSetting(golems);
        addSetting(others);

        addSetting(new SeparatorSetting("Additional Rules"));
        includeInvisible = new BooleanSetting("Include Invisible", "Allow invisible entities", Defaults.INCLUDE_INVISIBLE);
        includeDead = new BooleanSetting("Include Dead", "Allow entities with zero or less health", Defaults.INCLUDE_DEAD);
        addSetting(includeInvisible);
        addSetting(includeDead);
    }

    @Override
    public void toggle()
    {
        // Keep disabled; this module acts purely as a configuration container.
    }

    @Override
    public void setEnabled(boolean enabled, boolean showFeedback)
    {
        // Prevent enabling/disabling behaviour – settings are always respected.
    }

    public static Targets get()
    {
        if (instance == null)
        {
            ProtonClient client = ProtonClient.getInstance();
            ModuleManager manager = client != null ? client.getModuleManager() : null;
            Module module = manager != null ? manager.getModuleByName("Targets") : null;
            if (module instanceof Targets)
            {
                instance = (Targets) module;
            }
        }
        return instance;
    }

    public boolean isEntityAllowed(Entity entity)
    {
        if (!(entity instanceof EntityLivingBase))
        {
            return false;
        }

        EntityLivingBase living = (EntityLivingBase) entity;

        if (!includeDead.getValue() && !living.isEntityAlive())
        {
            return false;
        }

        if (!includeInvisible.getValue() && living.isInvisible())
        {
            return false;
        }

        if (living instanceof EntityPlayer)
        {
            return players.getValue();
        }

        if (living instanceof EntityMob)
        {
            return monsters.getValue();
        }

        if (living instanceof EntityAnimal || living instanceof EntityAmbientCreature || living instanceof EntitySquid)
        {
            return animals.getValue();
        }

        if (living instanceof EntityVillager)
        {
            return villagers.getValue();
        }

        if (living instanceof EntityIronGolem || living instanceof EntitySnowman)
        {
            return golems.getValue();
        }

        return others.getValue();
    }
}


