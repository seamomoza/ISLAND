package org.kotlin.island

import org.bukkit.*
import org.bukkit.entity.*
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.player.PlayerFishEvent
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.event.player.PlayerPortalEvent
import org.bukkit.event.world.TimeSkipEvent
import org.bukkit.plugin.java.JavaPlugin
import org.bukkit.scheduler.BukkitRunnable
import kotlin.random.Random

class Island : JavaPlugin(), Listener {

    override fun onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this)

    getCommand("isl")?.setExecutor { sender, command, label, args ->
            if (command.name == "isl" && args.size == 1 && args[0].equals("weather", ignoreCase = true)) {
                // 날씨 변경
                setRandomWeather()
                sender.sendMessage("날씨 변경!")
                true
            } else {
                false
            }
        }
    }

    // 랜덤으로 날씨 변경
    private fun setRandomWeather() {
        val world = Bukkit.getWorlds()[0]  // 첫 번째 월드를 가져옵니다
        if (world != null) {
            val randomWeather = Random.nextInt(3) // 0, 1, 2 중 하나를 랜덤으로 선택
            when (randomWeather) {
                0 -> {
                    world.setStorm(false)  // 맑은 날씨
                    world.weatherDuration = 0  // 날씨 지속 시간 초기화
                    // 맑은 날씨 타이틀 표시
                    sendWeatherTitle("🔅맑다🔅", "", 10, 70, 20)
                }

                1 -> {
                    world.setStorm(true)  // 비
                    world.weatherDuration = 6000  // 비의 지속시간 설정 (3000=15분, 6000=30분)
                    // 비 오는 날씨 타이틀 표시
                    sendWeatherTitle("💧비가 온다💧", "", 10, 70, 20)
                }

                2 -> {
                    world.setStorm(true)  // 폭풍우
                    world.weatherDuration = 10000  // 폭풍우 지속 시간 설정
                    // 폭풍우 타이틀 표시
                    sendWeatherTitle("⛈️폭풍우가 몰아친다⛈️", "", 10, 70, 20)

                }
            }
        }
    }

    // 타이틀 메시지를 보내는 함수
    private fun sendWeatherTitle(title: String, subtitle: String, fadeIn: Int, stay: Int, fadeOut: Int) {
        Bukkit.getOnlinePlayers().forEach { player ->
            player.sendTitle(title, subtitle, fadeIn, stay, fadeOut)
        }
    }

    @EventHandler
    fun onPlayerMove(event: PlayerMoveEvent) {
        val player = event.player
        val block = player.location.block
        if (block.type == Material.WATER || block.type == Material.AIR) {
            object : BukkitRunnable() {
                override fun run() {
                    if (player.isOnline && player.location.block.type == Material.WATER) {
                        player.noDamageTicks = 0  // 무적 시간 제거
                        player.damage(0.01)  // 즉시 5 데미지 적용
                    } else {
                        cancel()
                    }
                }
            }.runTaskTimer(this, 0L, 1L) // 2틱(0.1초)마다 반복
        }
    }


    @EventHandler
    fun onPlayerPortal(event: PlayerPortalEvent) {
        // 이동하려는 월드가 'nether'인 경우
        if (event.to.world?.name == "world_nether") {
            // 네더 포탈로 이동을 취소
            event.isCancelled = true
            // 플레이어에게 메시지 전송
            event.player.sendMessage("네더 포탈로 이동이 취소되었습니다.")
        }
    }

    @EventHandler
    fun onPlayerFish(event: PlayerFishEvent) {
        val player = event.player

        // 낚시에서 물고기가 잡히지 않고, 캐스팅이 완료된 경우에만 이벤트 처리
        if (event.state == PlayerFishEvent.State.CAUGHT_FISH) {
            // 10% 확률로 물고기를 없애고 몬스터를 튕겨낸다
            if (Random.nextInt(100) < 50) { // 10% 확률
                // 몬스터 튕겨내기
                val randomMonsterCount = Random.nextInt(1, 4) // 1~3마리
                for (i in 1..randomMonsterCount) {
                    spawnMonsterBounce(player)
                }
            }
        }
    }

    // 블레이즈 또는 엔더맨을 플레이어 방향으로 튕겨내는 함수
    private fun spawnMonsterBounce(player: Player) {
        // 플레이어의 위치에서 약간 위로 띄운 위치
        val spawnLocation = player.location.add(0.0, 1.0, 0.0)
        val randomMonster = Random.nextInt(2) // 0이면 블레이즈, 1이면 엔더맨

        // 소환 위치 설정
        val monster = when (randomMonster) {
            0 -> spawnLocation.world?.spawn(spawnLocation, Blaze::class.java)
            1 -> spawnLocation.world?.spawn(spawnLocation, Enderman::class.java)
            else -> null
        }

        monster?.let {
            // 몬스터가 튕겨나가도록 벡터를 설정 (플레이어의 방향으로 튕겨나감)
            val vector = player.location.direction.multiply(1.5) // 1.5배 빠르게
            it.velocity = vector
        }
    }

    @EventHandler
    fun onPlayerInteractEntity(event: PlayerInteractEntityEvent) {
        val player = event.player
        if (event.rightClicked is Boat) {
            event.isCancelled = true // 보트 탑승을 막음
            player.sendMessage("보트를 탈 수 없습니다.")
        }
    }

    @EventHandler
    fun onBlockPlace(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        val loc = player.location

        // 플레이어가 놓은 블록이 드래곤알이고, 드래곤알 이름이 "탈출!"인지 확인
        if (block.type == Material.DRAGON_EGG) {
            // 이름이 "탈출!"인 드래곤알만 허용
            // 스컬크 촉매 위에 드래곤알이 놓였는지 확인
            val belowBlock = block.location.subtract(0.0, 1.0, 0.0).block

            if (belowBlock.type == Material.SCULK_CATALYST) {
                // 파티클을 점차 모아주는 작업
                val task = object : BukkitRunnable() {
                    var count = 0
                    override fun run() {
                        if (count >= 100) { // 파티클이 모여서 폭죽 효과로 변경되는 타이밍
                            // 폭죽 효과와 파티클을 나타내는 곳
                            player.world.playSound(loc, Sound.ITEM_TOTEM_USE, 1f, 1f)
                            player.world.spawnParticle(Particle.TOTEM_OF_UNDYING, loc, 1, 0.5, 1.0, 0.5)

                            // 탈출 성공 타이틀
                            player.sendTitle("탈출성공!!", "", 10, 70, 20)

                            // 플레이어를 지정된 좌표로 텔레포트
                            player.teleport(Location(player.world, -1616.0, 90.0, -320.0))
                            cancel()
                        } else {
                            block.world.spawnParticle(
                                Particle.PORTAL,
                                block.location.add(0.0, 1.0, 0.0),
                                100,
                                0.3,
                                0.3,
                                0.3
                            )
                            count++
                        }
                    }
                }
                task.runTaskTimer(this, 0L, 2L)
            }
        }
    }

}