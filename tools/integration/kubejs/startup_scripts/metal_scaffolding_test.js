StartupEvents.registry('block', event => {
    event.create('metal_scaffolding').property(BlockProperties.BOTTOM).box(0, 0, 0, 16, 16, 16)
    event.create('metal_scaffolding_control').box(0, 0, 0, 16, 16, 16)
})
