
/*
 * PwnFilter -- Regex-based User Filter Plugin for Bukkit-based Minecraft servers.
 * Copyright (c) 2013 Pwn9.com. Tremor77 <admin@pwn9.com> & Sage905 <patrick@toal.ca>
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 */

package com.pwn9.PwnFilter.bukkit.listener;

import com.pwn9.PwnFilter.bukkit.DeathMessages;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.PlayerDeathEvent;

/**
 * Catch Death events to rewrite them with a custom message.
 *
 * @author ptoal
 * @version $Id: $Id
 */
public class PwnFilterEntityListener implements Listener {

    /**
     * <p>onEntityDeath.</p>
     *
     * @param event a {@link org.bukkit.event.entity.EntityDeathEvent} object.
     */
    @EventHandler(priority=EventPriority.LOWEST)
    public void onEntityDeath(EntityDeathEvent event) {
        if (!(event instanceof PlayerDeathEvent)) return;

        PlayerDeathEvent e = (PlayerDeathEvent)event;

        final Player player = (Player)event.getEntity();

        if (DeathMessages.killedPlayers.containsKey(player.getUniqueId())) {
            e.setDeathMessage(DeathMessages.killedPlayers.remove(player.getUniqueId()));
        }

    }

}
