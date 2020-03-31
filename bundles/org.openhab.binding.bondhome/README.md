# Bond Home Binding

This binding supports radio (RF 315-433MHz) and IR devices controlled by a (Bond Home Bridge)[https://bondhome.io/products/].
The Bond bridge and its associated app can control nearly any RF or IR device, but specializes in ceiling fans, fireplaces, and motorized shades.
The Bridge works by recording the RF or IR signal from the factory remote and replaying the command when requested.
Olibra, the makers of the Bond, also maintains a large database of common remotes meaning all commands from a particular remote can often be looked up after programming a single command.
This binding does not attempt to replicate the ability to program new remotes or commands - that should be done using the Bond app.
Instead, this binding mearly communicates with the Bridge in order to control the devices.

Keep in mind when using this binding that all of the limitations of the Bond Bridge and of using typical state-less ceiling fan remotes still apply.
That is - we have no way of being aware of any changes made using the physical device remote and toggle commands (like those for ceiling fan lights) may become out of sync with the real state of the light causing an "OFF" command to turn the light "ON" and vice-versa.

## Supported Things

This binding was developed and tested primarily for remote controlled ceiling fans, but any Bond-supported device should work.

The available thing types are:

| Device           | Serialized As | Thing Type       | Description |
|------------------|---------------|------------------|-------------|
| Bond Bridge      |               | bondBridge       | The RF/IR/Wifi Bridge |
| Ceiling Fan      | CF            | bondFan          | An RF or IR remote controlling a ceiling fan with or without a light |
| Motorized Shades | MS            | bondShades       | An RF or IR remote controlled fireplace with or without a fan |
| Fireplace        | FP            | bondFireplace    | An RF or IR remote controlled motorized shade |
| Generic device   | GX            | bondGenericThing | A generic RF or IR remote controlled device |

## Discovery

The Bond bridge itself cannot be discovered and must be manually created.
To create the bridge thing, you must have both the Bond bridge serial number and local token.
Both are viewable in the Bond app.
After manually creating the Bond Bridge thing, a scan can be done for any devices registered to that bridge.
The type of device (ie, fan vs shades vs fireplace) _should_ be detected at discovery, but auto-discovered devices might end up labeled as "ceiling fan" regardless.
In the case of a device being mistakenly discovered as a ceiling fan, try adding it manually.

## Binding Configuration

There is no configuration required for the binding itself.

## Thing Configuration

To create a Bond Bridge thing, both the device id and local token are required.
Both are viewable in the Bond app.


| Parameter ID  | Parameter Type | Mandatory | Description                                         | Default |
|---------------|----------------|-----------|-----------------------------------------------------|---------|
| bondId        | text           | true      | The serial number of the Bond bridge (ie ZZBL...)   |         |
| localToken    | text           | true      | The local access token for the bridge               |         |
| bondIpAddress | text           | false     | The IP of the bridge; attempts to detect if omitted |         |


Example Bridge:

```
Bridge bondhome:bondBridge:myBridge "My Bond Bridge" @ "Living Room" [ bondId="ZZBLxxx", localToken="abc123..." ]
```

To create any other device, only the bridge id and  device id is required.

| Parameter ID | Parameter Type | Mandatory | Description                             | Default |
|--------------|----------------|-----------|-----------------------------------------|---------|
| deviceId     | text           | true      | The device id, viewable in the Bond app |         |

Example Thing:

```
Thing bondhome:bondFan:bondBridge:myFan (bondhome:bondBridge:myBridge) "My Ceiling Fan" @ "Living Room" [deviceId = "abc1234"]
```

Example Bridge with multiple devices:

```
Bridge bondhome:bondBridge:myBridge "My Bond Bridge" @ "Living Room" [ bondId="ZZBLxxx", localToken="abc123..." ] {
    Thing bondFan          MyFan1   "My Ceiling Fan"   @ "Living Room" [deviceId = "abc1234"]
    Thing bondFan          MyFan2   "My Other Fan"     @ "Bedroom"     [deviceId = "abc1234"]
    Thing bondShades       MyShades "My Roller Shades" @ "Bedroom"     [deviceId = "abc1234"]
    Thing bondFireplace    MyFP     "My Fireplace"     @ "Bedroom"     [deviceId = "abc1234"]
    Thing bondGenericThing MyTV     "My TV"            @ "Living Room" [deviceId = "abc1234"]
}
```


## Channels

Exactly which channels are available will depend on the commands available for that remote.
During initialization, the binding queries the Bond bridge for possible commands and then deletes any channels that do not apply.

| Channel Group      | Channel           | Type     | Description                                        | Appies to    |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| commonChannels     | power                    | Switch   | Device power                                       | All devices  |
| commonChannels     | lastUpdate               | DateTime | Timestamp of last status update                    | All devices  |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| ceilingFanChannels | fanSpeed                 | Dimmer   | Sets fan speed                                     | Ceiling Fans |
| ceilingFanChannels | breezeState              | Dimmer   | Enables or disables breeze mode                    | Ceiling Fans |
| ceilingFanChannels | breezeMean               | Number   | Sets the average speed in breeze mode              | Ceiling Fans |
| ceilingFanChannels | breezeVariability        | Number   | Sets the variability of the speed in breeze mode   | Ceiling Fans |
| ceilingFanChannels | direction                | Switch   | Sets the fan direction; forward or reverse         | Ceiling Fans |
| ceilingFanChannels | timer                    | Number   | Starts a timer for s seconds                       | Ceiling Fans |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| lightChannels      | light                    | Switch   | Turns the light on the ceiling fan on or off       | Ceiling Fans |
| lightChannels      | brightness               | Dimmer   | Adjusts the brightness of the fan light            | Ceiling Fans |
| lightChannels      | dimmerStartStop          | Switch   | Starts changing the brightness of the fan light    | Ceiling Fans |
| lightChannels      | dimmerIncr               | Switch   | Starts increasing the brightness of the fan light  | Ceiling Fans |
| lightChannels      | dimmerDcr                | Switch   | Starts decreasing the brightness of the fan light  | Ceiling Fans |
| lightChannels      | stop                     | Switch   | Stops changing brightness                          | Ceiling Fans |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| upLightChannels    | upLight                  | Switch   | Turns the up-light on a fan on or off              | Ceiling Fans |
| upLightChannels    | upLightEnable            | Switch   | Enables or disables the up light\*                 | Ceiling Fans |
| upLightChannels    | upLightBrightness        | Dimmer   | Adjusts the brightness of the up light             | Ceiling Fans |
| upLightChannels    | upLightDimmerStartStop   | Switch   | Starts changing the brightness of the up light     | Ceiling Fans |
| upLightChannels    | upLightDimmerIncr        | Switch   | Starts increasing the brightness of the up light   | Ceiling Fans |
| upLightChannels    | upLightDimmerDcr         | Switch   | Starts decreasing the brightness of the up light   | Ceiling Fans |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| downLightChannels  | downLight                | Switch   | Turns the down-light on a fan on or off            | Ceiling Fans |
| downLightChannels  | downLightEnable          | Switch   | Enables or disables the down light\*               | Ceiling Fans |
| downLightChannels  | downLightBrightness      | Dimmer   | Adjusts the brightness of the down light           | Ceiling Fans |
| downLightChannels  | downLightDimmerStartStop | Switch   | Starts changing the brightness of the down light   | Ceiling Fans |
| downLightChannels  | downLightDimmerIncr      | Switch   | Starts increasing the brightness of the down light | Ceiling Fans |
| downLightChannels  | downLightDimmerDcr       | Switch   | Starts decreasing the brightness of the down light | Ceiling Fans |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| downLightChannels  | flame                    | Dimmer   | Turns on or adjust the flame level                 | Fireplaces   |
| downLightChannels  | fpFanPower               | Switch   | Turns the fireplace fan on or off                  | Fireplaces   |
| downLightChannels  | fpFanSpeed               | Dimmer   | Adjusts the speed of the fireplace fan             | Fireplaces   |
|--------------------|--------------------------|----------|----------------------------------------------------|--------------|
| shadeChannels      | openShade                | Switch   | Opens or closes motorize shades            | Motor Shades |
| shadeChannels      | hold                     | Switch   | Tells a device to stop moving               | Motor Shades   |

\* If the fan up or down light is "enabled" it will turn on when the "light" channel is turned on.

Note:  For fan lights, the brightness cannot generally be set to a given level, only changed from the current level.
