if (typeof VMDieTechnology === 'undefined') throw new Error('Missing VMDieTechnology server binding')

ServerEvents.loaded(event => {
    const FakePlayers = Java.loadClass('net.neoforged.neoforge.common.util.FakePlayerFactory')
    const Races = Java.loadClass('org.dishanhai.vmdiecore.monster.Race')
    const RaceEvents = Java.loadClass('org.dishanhai.vmdiecore.monster.RaceEvents')
    const player = FakePlayers.getMinecraft(event.server.overworld())
    player.setPos(0, 200, 0)
    if (!RaceEvents.choose(player, Races.VAMPIRE, true)) throw new Error('Fixture race selection failed')
    if (String(VMDieTechnology.getRace(player)) !== 'vampire') throw new Error('Race query failed')
    for (const technology of ['zombie', 'skeleton', 'enderman', 'arthropod']) {
        if (!VMDieTechnology.hasTech(player, technology)) throw new Error('Vampire missing technology: ' + technology)
    }
    RaceEvents.choose(player, Races.ZOMBIE, true)
    if (VMDieTechnology.hasTech(player, 'enderman')) throw new Error('Zombie bypassed technology gate')
    console.info('VMDieCore technology binding queries passed')
})
