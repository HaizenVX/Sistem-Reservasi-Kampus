const KUNCI = "siprf_reservasi";
const MENUNGGU = "Menunggu Persetujuan";

let RUANGAN = [];
let daftarRuangan = [];
async function muatRuangan() {
  daftarRuangan=await api('/api/ruangan');RUANGAN=daftarRuangan.map(r=>r.namaRuangan);
}
async function bacaReservasi() { return api('/api/reservasi'); }

function tersedia(ruangan, tanggal, waktu, data) {
  if (!RUANGAN.includes(ruangan) || !tanggal || !waktu) {
    return false;
  }

  const sudahDigunakan = data.some(reservasi =>
    reservasi.ruangan === ruangan &&
    reservasi.tanggal === tanggal &&
    reservasi.waktu === waktu &&
    reservasi.status === "Disetujui"
  );

  return !sudahDigunakan;
}

function labelStatus(teks) {
  const warna = {
    "Tersedia": "baik",
    "Tidak tersedia": "buruk",
    "Menunggu Persetujuan": "tunggu",
    "Disetujui": "baik",
    "Ditolak": "buruk"
  };

  const span = document.createElement("span");
  span.className = "label " + (warna[teks] || "");
  span.textContent = teks;

  return span;
}

function formatTanggal(tanggal) {
  if (!tanggal) {
    return "-";
  }

  const bagian = tanggal.split("-");
  const tahun = Number(bagian[0]);
  const bulan = Number(bagian[1]) - 1;
  const hari = Number(bagian[2]);

  const tanggalReservasi = new Date(tahun, bulan, hari);

  return tanggalReservasi.toLocaleDateString("id-ID", {
    day: "numeric",
    month: "long",
    year: "numeric"
  });
}