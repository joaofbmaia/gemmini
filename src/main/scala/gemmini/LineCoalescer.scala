package gemmini

import chisel3._
import chisel3.util._


// coalesces multiple words in a single line
// Big endian (First word is MSB)
class LineCoalescer(word_width: Int, line_width: Int) extends Module {
  val word_number = (line_width + word_width - 1) / word_width // div round up
  val word_sel_width = log2Ceil(word_number)
  
  val io = IO(new Bundle {
    val in = Flipped(ValidIO(UInt(word_width.W)))
    val out = ValidIO(UInt(line_width.W))
    val word_select = Input(UInt(word_sel_width.W))
  })

  val firstWord_width = if (word_number > 1) line_width % word_width else 0

  val firstWord = Seq(Reg(UInt(firstWord_width.W)))
  val midWords = Seq.fill((word_number - 2).max(0))(Reg(UInt(word_width.W)))
  val lastWord = Seq(Wire(UInt(word_width.W)))

  val words = firstWord ++ midWords ++ lastWord

  val word_select_1h = Wire(UInt(word_number.W))

  // one hot decoder
  word_select_1h := (1.U << io.word_select)

  for (i <- 0 until word_number - 1) {
    when (word_select_1h(i)) {
      words(i) := io.in.bits
    }
  }
  words.last := io.in.bits

  io.out.valid := word_select_1h(word_number - 1) && io.in.valid
  io.out.bits := Cat(words)
}