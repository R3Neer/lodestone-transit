# Gameplay messages (specified before implementation)

All messages are localized action-bar overlays, never chat. Linking/loading is not a travel attempt. Empty or unready devices do not spend fuel. Once a ready, fueled attempt begins, fuel and vanilla pearl damage apply even when destination validation fails.

| Situation | English | Spanish |
| --- | --- | --- |
| Still arming | Teleporter is still warming up. | El teletransportador aún se está preparando. |
| Empty portable or station | No ender pearls. Load a pearl to teleport. | No quedan perlas de ender. Carga una para teletransportarte. |
| Missing destination | No destination linked. | No hay ningún destino enlazado. |
| Unknown, destroyed or invalid anchor | Linked lodestone is no longer available. | La magnetita enlazada ya no está disponible. |
| Ordinary lodestone target | Linked lodestone is not calibrated. Teleportation unavailable. | La magnetita enlazada no está calibrada. Teletransporte no disponible. |
| Anchor currently moving | Linked lodestone is moving. Try again when it stops. | La magnetita enlazada se está moviendo. Inténtalo cuando se detenga. |
| Recovery without death | No last death location is available. | No hay una ubicación de última muerte disponible. |
| Destination dimension missing | The destination dimension is unavailable. | La dimensión de destino no está disponible. |
| Normal device across dimensions | Destination is in another dimension. A dimensional teleporter is required. | El destino está en otra dimensión. Necesitas un teletransportador dimensional. |
| Activator dead/removed/spectating | You cannot teleport in your current state. | No puedes teletransportarte en tu estado actual. |
| Activator sleeping | Wake up before teleporting. | Despiértate antes de teletransportarte. |
| No safe arrival for player | No safe arrival space near the destination. | No hay un lugar de llegada seguro cerca del destino. |
| No safe arrival for mount/passengers | Not enough safe arrival space for your mount and passengers. | No hay espacio de llegada seguro para tu montura y sus pasajeros. |
| Essential entity cannot travel | Your mount or a passenger cannot travel to this dimension. | Tu montura o uno de sus pasajeros no puede viajar a esta dimensión. |
| Transfer returned no entity | Teleportation could not be completed. | No se ha podido completar el teletransporte. |
| Secondary leash group left behind (partial success) | Some leashed entities could not follow you. | Algunas entidades atadas no han podido acompañarte. |
| Broken station block entity | This teleport station is unavailable. | Esta estación de teletransporte no está disponible. |
| Duplicate activation same tick | Ignore duplicate; preserve the first activation's message. | Ignorar el duplicado y conservar el mensaje de la primera activación. |
| Fuel full (loading feedback) | Ender pearl storage is full. | El depósito de perlas de ender está lleno. |
| Link to ordinary lodestone | Linked to uncalibrated lodestone. Teleportation unavailable. | Enlazado a una magnetita sin calibrar. Teletransporte no disponible. |
| Link to calibrated lodestone | Linked to calibrated lodestone. | Enlazado a una magnetita calibrada. |

Successful travel keeps its existing sound and particles. No generic failure message should overwrite a more specific reason. Unexpected programming exceptions must remain visible in logs rather than being silently swallowed.
