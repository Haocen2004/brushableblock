# Brushable Block

A Minecraft mod that extends the vanilla brushing mechanic to any block via tags.

## Tags
`brushableblock:brushable`

## Loot Tables
For any block, using loot table path `brushableblock:brushing/<block>` or `brushableblock:brushing/<namespace>/<block>` to define drops.   
Replace block with function `brushableblock:set_transformed_block`
```json
{
    "type": "minecraft:archaeology",
    "pools": [
        {
            "rolls": 1,
            "entries": [
                {
                    "type": "minecraft:item",
                    "name": "minecraft:diamond"
                }
            ],
            "functions": [
                {
                    "function": "brushableblock:set_transformed_block",
                    "block": "minecraft:bedrock"
                }
            ]
        }
    ]
}
```
## Config
```
#Choose the brushing progress overlay mode
#Allowed Values: NONE, VANILLA, CUSTOM
overlayMode = "NONE"
```
When `CUSTOM` MODE, will load `textures\block\brushing_overlay_[0-3].png`
