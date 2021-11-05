package com.nisovin.shopkeepers.util.bukkit;

import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.World;
import org.bukkit.entity.Player;

import com.nisovin.shopkeepers.util.data.DataContainer;
import com.nisovin.shopkeepers.util.data.property.BasicProperty;
import com.nisovin.shopkeepers.util.data.property.Property;
import com.nisovin.shopkeepers.util.data.serialization.DataSerializer;
import com.nisovin.shopkeepers.util.data.serialization.InvalidDataException;
import com.nisovin.shopkeepers.util.data.serialization.java.DataContainerSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.EnumSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.NumberSerializers;
import com.nisovin.shopkeepers.util.data.serialization.java.StringSerializers;
import com.nisovin.shopkeepers.util.java.Validate;

/**
 * The data that makes up a particular sound effect.
 * <p>
 * Instances of this class are immutable.
 * <p>
 * Only the {@link #getSound() Sound} or {@link #getSoundName() sound name} are required to be specified. Any other
 * data, such as the {@link #getCategory() sound category}, {@link #getPitch() pitch}, or {@link #getVolume() volume},
 * is optional: If these are not specified, the sound effect will use their respective defaults when played.
 * <p>
 * Playing sound effects with a {@link #getSound() Sound} or {@link #getSoundName() sound name} may be implemented
 * differently (Minecraft uses two different packets for them). Even if two sound effects refer to the same sound, one
 * by {@link Sound} and the other by sound name, they are not considered equal.
 * <p>
 * When deserializing a {@link SoundEffect}, we first try to map a given sound name to a server-known {@link Sound}.
 * This means that if the sound effect was using a sound name instead of a {@link Sound} before being serialized, the
 * deserialized sound effect may not be equal to the previously serialized sound effect.
 * <p>
 * The {@link #getSoundName() sound name} can be empty to indicate that the sound effect shall not be played. This
 * allows plugin users to disable certain sound effects inside the config, and allows the plugin to differentiate
 * between disabled sound effects and missing / unspecified sound effects. Another option to disable a sound effect is
 * to set its volume to zero (or a value close to zero).
 */
public final class SoundEffect {

	/**
	 * A {@link SoundEffect} that does not play any sound.
	 */
	public static final SoundEffect EMPTY = new SoundEffect("");

	private static final Property<String> SOUND_NAME = new BasicProperty<String>()
			.dataKeyAccessor("sound", StringSerializers.SCALAR)
			.build();
	private static final Property<SoundCategory> CATEGORY = new BasicProperty<SoundCategory>()
			.dataKeyAccessor("category", EnumSerializers.lenient(SoundCategory.class))
			.nullable() // Optional
			.defaultValue(SoundCategory.MASTER)
			.build();
	private static final Property<Float> PITCH = new BasicProperty<Float>()
			.dataKeyAccessor("pitch", NumberSerializers.FLOAT)
			.nullable() // Optional
			.defaultValue(1.0f)
			.build();
	private static final Property<Float> VOLUME = new BasicProperty<Float>()
			.dataKeyAccessor("volume", NumberSerializers.FLOAT)
			.nullable() // Optional
			.defaultValue(1.0f)
			.build();

	// Sound effects with a volume below this value are not played.
	private static final float MIN_VOLUME = 0.001f;

	/**
	 * A {@link DataSerializer} for values of type {@link SoundEffect}.
	 */
	public static final DataSerializer<SoundEffect> SERIALIZER = new DataSerializer<SoundEffect>() {
		@Override
		public Object serialize(SoundEffect value) {
			Validate.notNull(value, "value is null");
			Sound sound = value.getSound();
			String soundName = value.getSoundName();
			SoundCategory category = value.getCategory();
			Float pitch = value.getPitch();
			Float volume = value.getVolume();

			// Sound and sound name are both serialized as String:
			String serializedSound = (sound != null) ? sound.name() : soundName;

			// Use a compact representation if only the sound / sound name is specified:
			if (category == null && pitch == null && volume == null) {
				return serializedSound;
			}

			DataContainer soundEffectData = DataContainer.create();
			soundEffectData.set(SOUND_NAME, serializedSound);
			soundEffectData.set(CATEGORY, category);
			soundEffectData.set(PITCH, pitch);
			soundEffectData.set(VOLUME, volume);
			return soundEffectData.serialize();
		}

		@Override
		public SoundEffect deserialize(Object data) throws InvalidDataException {
			Validate.notNull(data, "data is null");
			String soundName = null;
			SoundCategory category = null;
			Float pitch = null;
			Float volume = null;

			if (data instanceof String) {
				// Compact representation:
				soundName = (String) data; // Can be empty
			} else {
				DataContainer soundEffectData = DataContainerSerializers.DEFAULT.deserialize(data);
				soundName = soundEffectData.get(SOUND_NAME); // Can be empty
				category = soundEffectData.get(CATEGORY);
				pitch = soundEffectData.get(PITCH);
				volume = soundEffectData.get(VOLUME);
			}

			// Check if the sound name matches a known Sound:
			Sound sound = MinecraftEnumUtils.parseEnum(Sound.class, soundName); // Can be null
			if (sound != null) {
				// We use the Sound instead of the sound name.
				soundName = null;
			}

			return new SoundEffect(sound, soundName, category, pitch, volume);
		}
	};

	// Either sound or soundName is not null. Sound name is used when referring to a sound that is not known to the
	// server and might only exist on (certain) clients.
	private final Sound sound; // Can be null, not null if soundName is null
	private final String soundName; // Can be null, not null if sound is null, empty to not play any sound
	private final SoundCategory category; // Null if not specified
	private final Float pitch; // Null if not specified
	private final Float volume; // Null if not specified

	/**
	 * Creates a new {@link SoundEffect}.
	 * 
	 * @param sound
	 *            the sound, not <code>null</code>
	 */
	public SoundEffect(Sound sound) {
		this(sound, null, null, null, null);
	}

	/**
	 * Creates a new {@link SoundEffect}.
	 * 
	 * @param soundName
	 *            the sound name, not <code>null</code>, can be empty to not play any sound
	 */
	public SoundEffect(String soundName) {
		this(null, soundName, null, null, null);
	}

	/**
	 * Creates a new {@link SoundEffect}.
	 * 
	 * @param sound
	 *            the sound, or <code>null</code> iff {@code soundName} is not <code>null</code>
	 * @param soundName
	 *            the sound name, or <code>null</code> iff {@code sound} is not <code>null</code>, can be empty to not
	 *            play any sound
	 * @param category
	 *            the sound category, or <code>null</code> if not specified
	 * @param pitch
	 *            the pitch, or <code>null</code> if not specified
	 * @param volume
	 *            the volume, or <code>null</code> if not specified
	 */
	private SoundEffect(Sound sound, String soundName, SoundCategory category, Float pitch, Float volume) {
		if (sound != null) {
			Validate.isTrue(soundName == null, "sound and soundName are both non-null");
		} else {
			Validate.notNull(soundName, "sound and soundName are both null");
		}
		this.sound = sound;
		this.soundName = soundName;
		this.category = category;
		this.pitch = pitch;
		this.volume = volume;
	}

	/**
	 * Creates a new {@link SoundEffect} that copies the data of this {@link SoundEffect} but uses the specified
	 * {@link SoundCategory}.
	 * 
	 * @param category
	 *            the sound category, or <code>null</code> to indicate 'unspecified'
	 * @return the new sound effect
	 */
	public SoundEffect withCategory(SoundCategory category) {
		return new SoundEffect(sound, soundName, category, pitch, volume);
	}

	/**
	 * Creates a new {@link SoundEffect} that copies the data of this {@link SoundEffect} but uses the specified pitch.
	 * 
	 * @param pitch
	 *            the pitch, or <code>null</code> to indicate 'unspecified'
	 * @return the new sound effect
	 */
	public SoundEffect withPitch(Float pitch) {
		return new SoundEffect(sound, soundName, category, pitch, volume);
	}

	/**
	 * Creates a new {@link SoundEffect} that copies the data of this {@link SoundEffect} but uses the specified volume.
	 * 
	 * @param volume
	 *            the volume, or <code>null</code> to indicate 'unspecified'
	 * @return the new sound effect
	 */
	public SoundEffect withVolume(Float volume) {
		return new SoundEffect(sound, soundName, category, pitch, volume);
	}

	/**
	 * Gets the {@link Sound}.
	 * 
	 * @return the sound, or <code>null</code> if a {@link #getSoundName() sound name} is used
	 */
	public Sound getSound() {
		return sound;
	}

	/**
	 * Gets the sound name.
	 * <p>
	 * This sound name may refer to a sound that only exists on (certain) clients. It might not correspond to a known
	 * {@link Sound}.
	 * 
	 * @return the sound name, or <code>null</code> if a {@link #getSound() Sound} is used, can be empty to indicate to
	 *         not play any sound
	 */
	public String getSoundName() {
		return soundName;
	}

	/**
	 * Gets the {@link SoundCategory} stored by this {@link SoundEffect}.
	 * 
	 * @return the sound category, or <code>null</code> if not specified
	 */
	public SoundCategory getCategory() {
		return category;
	}

	/**
	 * Gets the effective {@link SoundCategory}.
	 * <p>
	 * If this {@link SoundEffect} specifies no category, this returns the default category
	 * {@link SoundCategory#MASTER}.
	 * 
	 * @return the sound category, not <code>null</code>
	 */
	public SoundCategory getEffectiveCategory() {
		return (category != null) ? category : CATEGORY.getDefaultValue();
	}

	/**
	 * Gets the pitch stored by this {@link SoundEffect}.
	 * 
	 * @return the pitch, or <code>null</code> if not specified
	 */
	public Float getPitch() {
		return pitch;
	}

	/**
	 * Gets the effective pitch.
	 * <p>
	 * If this {@link SoundEffect} specifies no pitch, this returns the default pitch {@code 1.0f}.
	 * 
	 * @return the pitch
	 */
	public float getEffectivePitch() {
		return (pitch != null) ? pitch : PITCH.getDefaultValue();
	}

	/**
	 * Gets the volume stored by this {@link SoundEffect}.
	 * 
	 * @return the volume, or <code>null</code> if not specified
	 */
	public Float getVolume() {
		return volume;
	}

	/**
	 * Gets the effective volume.
	 * <p>
	 * If this {@link SoundEffect} specifies no volume, this returns the default volume {@code 1.0f}.
	 * 
	 * @return the volume
	 */
	public float getEffectiveVolume() {
		return (volume != null) ? volume : VOLUME.getDefaultValue();
	}

	/**
	 * Checks whether this {@link SoundEffect} is disabled.
	 * <p>
	 * The sound effect is disabled if either its {@link #getSoundName() sound name} is empty, or its
	 * {@link #getVolume() volume} is close to zero. A disabled sound effect is not played by any of the play methods.
	 * 
	 * @return <code>true</code> if this sound effect is disabled
	 */
	public boolean isDisabled() {
		return (soundName != null && soundName.isEmpty()) || (volume != null && volume <= MIN_VOLUME);
	}

	/**
	 * Plays this {@link SoundEffect} at the specified {@link Location}.
	 * 
	 * @param location
	 *            the location
	 * @see World#playSound(Location, String, SoundCategory, float, float)
	 */
	public void play(Location location) {
		Validate.notNull(location, "location is null");
		World world = location.getWorld();
		Validate.notNull(world, "World of location is null");
		if (this.isDisabled()) return;

		if (sound != null) {
			world.playSound(location, sound, this.getEffectiveCategory(), this.getEffectiveVolume(), this.getEffectivePitch());
		} else {
			assert !soundName.isEmpty();
			world.playSound(location, soundName, this.getEffectiveCategory(), this.getEffectiveVolume(), this.getEffectivePitch());
		}
	}

	/**
	 * Plays this {@link SoundEffect} for the specified player, at its {@link Player#getEyeLocation()}.
	 * 
	 * @param player
	 *            the player
	 * @see #play(Player, Location)
	 */
	public void play(Player player) {
		Validate.notNull(player, "player is null");
		this.play(player, player.getEyeLocation());
	}

	/**
	 * Plays this {@link SoundEffect} for the specified player, at the specified {@link Location}.
	 * 
	 * @param player
	 *            the player
	 * @param location
	 *            the location
	 * @see Player#playSound(Location, String, SoundCategory, float, float)
	 */
	public void play(Player player, Location location) {
		Validate.notNull(player, "player is null");
		Validate.notNull(location, "location is null");
		if (this.isDisabled()) return;

		if (sound != null) {
			player.playSound(location, sound, this.getEffectiveCategory(), this.getEffectiveVolume(), this.getEffectivePitch());
		} else {
			assert !soundName.isEmpty();
			player.playSound(location, soundName, this.getEffectiveCategory(), this.getEffectiveVolume(), this.getEffectivePitch());
		}
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("SoundEffect [sound=");
		builder.append(sound);
		builder.append(", soundName=");
		builder.append(soundName);
		builder.append(", category=");
		builder.append(category);
		builder.append(", pitch=");
		builder.append(pitch);
		builder.append(", volume=");
		builder.append(volume);
		builder.append("]");
		return builder.toString();
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((sound == null) ? 0 : sound.hashCode());
		result = prime * result + ((soundName == null) ? 0 : soundName.hashCode());
		result = prime * result + ((category == null) ? 0 : category.hashCode());
		result = prime * result + ((pitch == null) ? 0 : pitch.hashCode());
		result = prime * result + ((volume == null) ? 0 : volume.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (!(obj instanceof SoundEffect)) return false;
		SoundEffect other = (SoundEffect) obj;
		if (sound != other.sound) return false;
		if (soundName == null) {
			if (other.soundName != null) return false;
		} else if (!soundName.equals(other.soundName)) return false;
		if (category != other.category) return false;
		if (pitch == null) {
			if (other.pitch != null) return false;
		} else if (!pitch.equals(other.pitch)) return false;
		if (volume == null) {
			if (other.volume != null) return false;
		} else if (!volume.equals(other.volume)) return false;
		return true;
	}

	/**
	 * Serializes this {@link SoundEffect}.
	 * 
	 * @return the serialized {@link SoundEffect}
	 */
	public Object serialize() {
		return SERIALIZER.serialize(this);
	}
}
