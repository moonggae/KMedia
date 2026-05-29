package io.github.moonggae.kmedia.sample

import io.github.moonggae.kmedia.sample.model.SampleMusic

class SampleMusicRepository {
    fun getSampleMusicList(): List<SampleMusic> {
        return headerSmokeMusicList() + listOf(
            SampleMusic(
                id = "1c05b730-1455-40d2-b84c-30faf529de76",
                title = "Stutterfly",
                artist = "Spektrem",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/857/1000x0/stutterfly-1740704455-NQ13SD2ADl.jpg",
                uri = ""
            ),
            SampleMusic(
                id = "c4fe8d10-a9d2-4ce4-855b-19afc9eb9edc",
                title = "Buried Alive",
                artist = "REEBZ, Badlokk",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/856/1000x0/1740651926_hwGpEEMIUg_Buries_Alive_FINAL_shrunk.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/856/buried-alive-1740618057-loR9j84eZ6.mp3"
            ),
            SampleMusic(
                id = "76fc0203-42e7-424b-b030-650fa7bd316b",
                title = "Back Again",
                artist = "ThatBehavior",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/855/1000x0/back-again-1740445258-UbVnFb4lC8.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/855/back-again-1740445261-k3fcXySPR1.mp3"
            ),
            SampleMusic(
                id = "ebc7891f-9b5e-4be3-8f38-04d9ce64f222",
                title = "Another Way",
                artist = "KDH, Syn Cole, Vikkstar, Joe Jury",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/851/1000x0/another-way-1740099656-K3fjmMaGPH.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/851/another-way-1740099658-8uAHVXn6qb.mp3"
            ),
            SampleMusic(
                id = "17d5cee5-87dd-4edb-967c-4c6038a51fff",
                title = "Skyline Pt. II",
                artist = "Electro-Light, Kovan",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/837/1000x0/skyline-pt-ii-1737457256-pawiAl5d7U.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/837/skyline-pt-ii-1737457258-w7Fx4Orpbo.mp3"
            ),
            SampleMusic(
                id = "4ae2238d-e022-431d-b014-b48528cb61ac",
                title = "Mortals Funk Remix (Sped Up)",
                artist = "Warriyo, LXNGVX",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/833/1000x0/mortals-funk-remix-sped-up-1737075655-r7mO3BxdHM.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/833/mortals-funk-remix-sped-up-1737075656-1qquCx7EwV.mp3"
            ),
            SampleMusic(
                id = "c093db32-b195-4e14-a988-a358841849e9",
                title = "Mortals Funk Remix (Super Slowed)",
                artist = "Warriyo, LXNGVX",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/834/1000x0/mortals-funk-remix-super-slowed-1737075660-kxuVW78tzm.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/834/mortals-funk-remix-super-slowed-1737075662-It4xbTDYpy.mp3"
            ),
            SampleMusic(
                id = "6d9a7866-a655-454d-b9a9-7af0a179f3d9",
                title = "CHEAT CODES",
                artist = "TOKYO MACHINE",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/817/1000x0/cheat-codes-1734051653-hHkS55rL0V.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/817/cheat-codes-1734051655-NotfduweTh.mp3"
            ),
            SampleMusic(
                id = "49d4a22d-5bd4-48b5-b690-23f935073e26",
                title = "I Can Feel",
                artist = "Syn Cole",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/816/1000x0/i-can-feel-1733965256-YXbmlkiuMp.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/816/i-can-feel-1733965258-CUZGpIG30o.mp3"
            ),
            SampleMusic(
                id = "73101dd0-c319-494a-96d9-6e48de9594f2",
                title = "What You Did",
                artist = "eerie, Rameses B",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/815/1000x0/what-you-did-1733792458-vYVprGNPBu.png",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/815/what-you-did-1733792461-IunWtNAKbg.mp3"
            ),
            SampleMusic(
                id = "598a4b7b-7980-4d27-912e-81ca14fec7ca",
                title = "Strobe",
                artist = "NIVIRO",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/813/1000x0/strobe-1733446856-PCrTkLuaWv.png",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/813/strobe-1733446858-c4mvlbTKTf.mp3"
            ),
            SampleMusic(
                id = "13d34597-2546-46a4-9842-3ba06425f170",
                title = "RUINS",
                artist = "DJ FKU, LXNGVX",
                coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/814/1000x0/ruins-1733446864-4gin1pwuwN.jpg",
                uri = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/814/ruins-1733446865-LQlnvzERhj.mp3"
            )
        )
    }

    private fun headerSmokeMusicList(): List<SampleMusic> =
        if (HEADER_SMOKE_URL.isBlank()) {
            emptyList()
        } else {
            listOf(
                SampleMusic(
                    id = "kmedia-header-smoke",
                    title = "Header Smoke",
                    artist = "Local capture server",
                    coverUrl = "https://ncsmusic.s3.eu-west-1.amazonaws.com/tracks/000/001/857/1000x0/stutterfly-1740704455-NQ13SD2ADl.jpg",
                    uri = HEADER_SMOKE_URL,
                    mimeType = "audio/mpeg",
                    requestHeaders = mapOf(
                        "Authorization" to "Bearer smoke-auth",
                        "X-Playback-Token" to "smoke-playback",
                    ),
                )
            )
        }

    companion object {
        // Android emulator: "http://10.0.2.2:18081/audio.mp3"
        // iOS simulator: "http://127.0.0.1:18081/audio.mp3"
        private const val HEADER_SMOKE_URL = ""
    }
}
