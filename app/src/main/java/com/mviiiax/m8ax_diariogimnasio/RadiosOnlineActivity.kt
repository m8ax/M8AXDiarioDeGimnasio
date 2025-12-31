package com.mviiiax.m8ax_diariogimnasio

import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class RadiosOnlineActivity : AppCompatActivity() {
    data class Radio(val nombre: String, val url: String)

    private var radioReproduciendo: Int = -1
    private val radios = listOf(
        Radio("1. La Cadena Dial", "https://25543.live.streamtheworld.com/CADENADIAL.mp3"),
        Radio(
            "2. Radio Marca Nacional",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/RADIOMARCA_NACIONAL.mp3"
        ),
        Radio("3. La Cadena Ser", "https://25493.live.streamtheworld.com/CADENASER.mp3"),
        Radio(
            "4. BBC World Service", "https://stream.live.vc.bbcmedia.co.uk/bbc_world_service"
        ),
        Radio(
            "5. Onda Cero, La Rioja",
            "https://atres-live.ondacero.es/live/delegaciones/oc/logrono/master.m3u8"
        ),
        Radio(
            "6. Radio Nacional De España", "https://dispatcher.rndfnk.com/crtve/rne1/rio/mp3/high"
        ),
        Radio(
            "7. Radio Exterior De España",
            "https://dispatcher.rndfnk.com/crtve/rneree/main/mp3/high"
        ),
        Radio("8. Radio Paradise", "https://stream.radioparadise.com/aac-320"),
        Radio("9. Radio 5 FM", "https://dispatcher.rndfnk.com/crtve/rne5/rio/mp3/high"),
        Radio("10. Canal Fiesta Radio", "https://rtva-live-radio.flumotion.com/rtva/cfr.mp3"),
        Radio(
            "11. Los 40 Principales",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/Los40.mp3"
        ),
        Radio(
            "12. Los 40 Urban",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/LOS40_URBAN.mp3"
        ),
        Radio("13. Europa FM", "https://atres-live.europafm.com/live/europafm/master.m3u8"),
        Radio(
            "14. Europa FM, Guipúzcoa",
            "https://stream-156.zeno.fm/se76qau1hc9uv?zt=eyJhbGciOiJIUzI1NiJ9.eyJzdHJlYW0iOiJzZTc2cWF1MWhjOXV2IiwiaG9zdCI6InN0cmVhbS0xNTYuemVuby5mbSIsInJ0bCI6NSwianRpIjoieHl6SWVsRG9UVUNZbzl5NFdTRTZzZyIsImlhdCI6MTczMDQ5NTc1OSwiZXhwIjoxNzMwNDk1ODE5fQ.rCKmfp2ohVvoFtuGkg1DHdsGHsrYURlwch7tb08Rf1o"
        ),
        Radio(
            "15. Europa FM, La Rioja",
            "https://atres-live.europafm.com/live/delegaciones/efm/logrono/master.m3u8"
        ),
        Radio(
            "16. Pure Ibiza Radio", "https://pureibizaradio.clubbingradios.com:9518/PureIbizaRadio"
        ),
        Radio("17. Ibiza Sonica Club", "https://ibizasonica.streaming-pro.com:8011/sonicaclub"),
        Radio("18. Ibiza Sonica", "https://ibizasonica.streaming-pro.com:8000/ibizasonica"),
        Radio("19. Best Ibiza Deep House", "https://stream.zeno.fm/lwv6zqgtv1dtv"),
        Radio("20. Loca FM, Años 90", "https://s2.we4stream.com/listen/loca_90s_/live"),
        Radio("21. Loca FM, Tech House", "https://s2.we4stream.com/listen/loca_tech_house/live"),
        Radio("22. Loca FM, Techno", "https://s2.we4stream.com/listen/loca_techo/live"),
        Radio("23. Classic Vinyl HD", "https://icecast.walmradio.com:8443/classic"),
        Radio("24. Mega Star", "https://megastar-cope.flumotion.com/playlist.m3u8"),
        Radio("25. Melodia FM", "https://atres-live.melodia-fm.com/live/melodiafm/master.m3u8"),
        Radio(
            "26. Greatest Classical Music",
            "https://az1.mediacp.eu/listen/100greatestclassicalmusic/radio.mp3"
        ),
        Radio("27. Rock FM", "https://rockfm-cope.flumotion.com/playlist.m3u8"),
        Radio(
            "28. Tomorrow Land, Radio",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/OWR_INTERNATIONAL_ADP.m3u8"
        ),
        Radio(
            "29. Hit FM",
            "https://adhandler.kissfmradio.cires21.com/get_link?url=https://bbhitfm.kissfmradio.cires21.com/bbhitfm.mp3"
        ),
        Radio(
            "30. Top Radio Madrid",
            "https://playerservices.streamtheworld.com/api/livestream-redirect/TOPRADIOAAC.aac"
        ),
        Radio(
            "31. Adroit Jazz", "https://icecast.walmradio.com:8443/jazz"
        ),
        Radio(
            "32. Adrenaline Tech House Radio", "https://streamer.radio.co/sa77aa975e/listen"
        ),
        Radio(
            "33. Mangoradio", "https://mangoradio.stream.laut.fm/mangoradio"
        ),
        Radio("34. United Music Cinema", "https://icy.unitedradio.it/um058.mp3"),
        Radio("35. 50s 60s Retro Hits", "https://stream.zeno.fm/pxzwykxbluitv"),
        Radio("36. Christmas Vinyl HD", "https://icecast.walmradio.com:8443/christmas"),
        Radio(
            "37. Funky Radio - (60's 70's)", "https://funkyradio.streamingmedia.it/play.mp3"
        ),
        Radio(
            "38. Rolling Stones", "https://streaming.exclusive.radio/er/rollingstones/icecast.audio"
        ),
        Radio("39. Classic Rock 70s 80s 90s", "https://cast1.torontocast.com:4610/stream"),
        Radio("40. 1940s Radio", "https://cast2.asurahosting.com/proxy/1940sradio/stream")
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_radios_online)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                AlertDialog.Builder(this).setTitle("Permiso De Notificaciones Requerido")
                    .setMessage("Para Que La Aplicación Pueda Mostrar La Notificación Con Controles De Reproducción, Necesito Que Permitas Las Notificaciones...")
                    .setPositiveButton("Permitir") { _, _ ->
                        requestPermissions(
                            arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 5678
                        )
                    }.setNegativeButton("No Permitir") { _, _ ->
                        Toast.makeText(this, "No Se Mostrará La Notificación", Toast.LENGTH_LONG)
                            .show()
                    }.show()
            }
        }
        val rvRadios = findViewById<RecyclerView>(R.id.rvRadios)
        rvRadios.layoutManager = LinearLayoutManager(this)
        rvRadios.adapter = RadioAdapter(radios) { index, radio ->
            if (radioReproduciendo == index) {
                stopRadio()
                radioReproduciendo = -1
            } else {
                if (radioReproduciendo != -1) stopRadio()
                playRadio(radio.url)
                radioReproduciendo = index
            }
            rvRadios.adapter?.notifyDataSetChanged()
        }
        findViewById<Button>(R.id.btnStop).setOnClickListener {
            stopRadio()
            radioReproduciendo = -1
            rvRadios.adapter?.notifyDataSetChanged()
        }
    }

    private fun playRadio(url: String) {
        val intent = Intent(this, RadioService::class.java)
        intent.action = RadioService.ACTION_PLAY
        intent.putExtra(RadioService.EXTRA_URL, url)
        startService(intent)
        Toast.makeText(this, "Reproduciendo Radio", Toast.LENGTH_SHORT).show()
    }

    private fun stopRadio() {
        val intent = Intent(this, RadioService::class.java)
        intent.action = RadioService.ACTION_STOP
        startService(intent)
        Toast.makeText(this, "Radio Detenida", Toast.LENGTH_SHORT).show()
    }

    inner class RadioAdapter(
        private val radios: List<Radio>, private val onClick: (Int, Radio) -> Unit
    ) : RecyclerView.Adapter<RadioAdapter.RadioViewHolder>() {
        inner class RadioViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvNombre: TextView = view.findViewById(R.id.tvNombreRadio)
            val btnPlay: ImageView = view.findViewById(R.id.btnPlayRadio)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RadioViewHolder {
            val view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_radio, parent, false)
            return RadioViewHolder(view)
        }

        override fun onBindViewHolder(holder: RadioViewHolder, position: Int) {
            val radio = radios[position]
            holder.tvNombre.text = radio.nombre
            if (position == radioReproduciendo) {
                holder.btnPlay.setImageResource(android.R.drawable.ic_media_pause)
            } else {
                holder.btnPlay.setImageResource(android.R.drawable.ic_media_play)
            }
            holder.itemView.setOnClickListener { onClick(position, radio) }
            holder.btnPlay.setOnClickListener { onClick(position, radio) }
        }

        override fun getItemCount() = radios.size
    }
}