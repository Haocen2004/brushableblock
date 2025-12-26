# Brushable Block

A Minecraft mod that extends the vanilla brushing mechanic to any block via tags.

## Tags
`brushableblock:brushable`

## Loot Tables
For any block, using loot table path `brushableblock:brushing/<block>` (without namespace) to define drops.   
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

