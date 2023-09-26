package gemmini

import chisel3._
import chisel3.util._

// StreamResizer Factory
object StreamResizer {
  def apply(M: Int, N: Int) =
    if (N > M) new StreamUpsizer(M, N) else new StreamTruncator(M, N)
}

abstract class StreamResizer(M: Int, N: Int) extends Module{
  val io = IO(new Bundle {
    val in = Flipped(ValidIO(UInt(M.W)))
    val out = ValidIO(UInt(N.W))
    val first = Input(Bool())
  })

  val ratio: Int
}


// big-endian
// upsizes a M-bit stream to a N-bit stream. Produces a valid N-bit stream element every divRoundUp(N,M) cycles
class StreamUpsizer(M: Int, N: Int) extends StreamResizer(M, N) {
  assert(N > M)

  val ratio = (N + M - 1) / M // div round up

  val remainder = N % M
  
  val counter = RegInit(1.U(ratio.W))

  val firstData = Seq(Reg(UInt(remainder.W)))

  val midData = Seq.fill(ratio - 2)(Reg(UInt(M.W)))

  val lastData = Seq(Wire(UInt(M.W)))

  val data = firstData ++ midData ++ lastData

  when (counter(0) || io.first) {
    data(0) := io.in.bits
  }

  for (i <- 1 until ratio - 1) {
    when (counter(i)) {
      data(i) := io.in.bits
    }
  }

  data.last := io.in.bits

  when(io.first && io.in.valid) {
    counter := 1.U(ratio.W).rotateLeft(1)
  } .elsewhen(io.in.valid) {
    counter := counter.rotateLeft(1)
  }

  io.out.valid := counter(ratio - 1) && io.in.valid && !io.first

  io.out.bits := Cat(data)
}

class StreamTruncator(M: Int, N: Int) extends StreamResizer(M, N) {
  assert(N <= M)

  val ratio = 1

  io.out.bits := io.in.bits
  io.out.valid := io.in.valid
}