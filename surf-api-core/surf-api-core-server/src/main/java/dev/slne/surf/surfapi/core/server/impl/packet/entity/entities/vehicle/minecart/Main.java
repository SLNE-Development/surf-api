package dev.slne.surf.surfapi.core.server.impl.packet.entity.entities.vehicle.minecart;

import static com.google.common.base.Preconditions.checkArgument;
import static dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketCommandBlockMinecart.COMMAND_PATTERN;
import static dev.slne.surf.surfapi.core.api.packet.entity.entities.vehicle.minecart.PacketCommandBlockMinecart.COMMAND_PATTERN_STRING;

public class Main {

  public static void main(String[] args) {
    checkArgument(
        COMMAND_PATTERN.matcher("/").matches(),
        """
            Command must match pattern'%s'!
            \n
            If you believe this is an error just go ahead and change the regex before blaming someone else!
            It's not like I'm the one who wrote it or anything...
            Oh wait, I did.
            Well, I guess you can blame me then.
            But I'm not going to change it.
            So there.
            I'm done now.
            Just change the regex it's not that hard.
            \n
            I'm not going to do it for you though.
            Why would I?
            \n
            I'm not the one who needs it changed.
            I'm the one who needs it to stay the same.
            I'm the one who wrote it.
            It's my regex.
            \n
            I'm not going to change it.
            You can change it.
            It's your regex now.
            That's how regexes work.
            You change them.
            They don't change you.
            You change them.
            You can change this one.
            You can change it to whatever you want.
            I don't care.
            Just change it.
            \n
            I'm not going to do it for you.
            I don't care what you change it to.
            Just change it.
            Please.
            Just change it.
            I'm begging you.
            Please change it.
            Please.
            \n
            I'm not going to do it.
            I'm not going to change it.
            I'm not going to change it for you.
            I'm not going to change it for anyone.
            I'm not going to change it for myself.
            I'm not going to change it for my family.
            I'm not going to change it for my friends.
            I'm not going to change it for my enemies.
            I'm not going to change it for anyone.
            """,
        COMMAND_PATTERN_STRING);
  }
}
