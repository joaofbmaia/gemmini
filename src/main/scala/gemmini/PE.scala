package gemmini

import chisel3._
import chisel3.util._
import chipsalliance.rocketchip.config

class PEReconfigurationControl[T <: Data](interconnectConfig: InterconnectConfig[T], controlPatternTableSize: Int) extends Bundle {
  val write_enable = Bool()
  val line_index = UInt(log2Ceil(controlPatternTableSize).W)
  // each line is divided two words: HIGH WORD when 0 and LOW WORD when 1
  val word_sel = UInt(1.W)
}

/**
  * PE contains CPG and ModPE
  * to reconfigure CPG, first write high part with word_sel = 0 and then, when when word_sel = 1 the low part combined with the previous high part are writen to the line
  */
class PE[T <: Data](interconnectConfig : InterconnectConfig[T], controlPatternTableSize: Int)
                   (implicit ev: Arithmetic[T]) extends Module { // Debugging variables
  import ev._

  val io = IO(new Bundle {
    val in_v_bcast = Input(interconnectConfig.verticalBroadcastType)
    val in_h_bcast = Input(interconnectConfig.horizontalBroadcastType)
    val out_v_bcast = Output(interconnectConfig.verticalBroadcastType)
    val out_h_bcast = Output(interconnectConfig.horizontalBroadcastType)
    val in_v = Input(interconnectConfig.interPEType)
    val in_h = Input(interconnectConfig.interPEType)
    val in_d = Input(interconnectConfig.interPEType)
    val out = Output(interconnectConfig.interPEType)

    val in_valid = Input(Bool())

    // which control pattern to use
    val next_control_pattern_index = Input(UInt(log2Ceil(controlPatternTableSize).W))

    // true when the control pattern to use changes
    val new_control_pattern = Input(Bool())

    // reconfiguration
    val rcgf = Input(new PEReconfigurationControl(interconnectConfig, controlPatternTableSize))

  })
  val mod_pe = Module(new ModPE(interconnectConfig))
  val cpg = Module(new PEControlPatternGenerator(interconnectConfig, controlPatternTableSize))

  val valid = io.in_valid
  
  // ModPE
  mod_pe.io.in_v_bcast := io.in_v_bcast
  mod_pe.io.in_h_bcast := io.in_h_bcast
  io.out_v_bcast := mod_pe.io.out_v_bcast
  io.out_h_bcast := mod_pe.io.out_h_bcast
  mod_pe.io.in_v := io.in_v
  mod_pe.io.in_h := io.in_h
  mod_pe.io.in_d := io.in_d
  io.out := mod_pe.io.out
  mod_pe.io.valid := valid

  // Control Pattern Generator
  cpg.io.valid := valid
  cpg.io.next_control_pattern_index := io.next_control_pattern_index
  cpg.io.new_control_pattern := io.new_control_pattern

  // conncet cpg to modPE
  mod_pe.io.control := cpg.io.pe_control


  // reconfiguration
  cpg.io.write_index := io.rcgf.line_index

  val word_width = interconnectConfig.verticalBroadcastType.getWidth
  val line_width = (new ControlPatternTableLine(interconnectConfig, controlPatternTableSize)).getWidth

  assert(line_width > word_width && line_width <= 2 * word_width, "vertical broadcast line width not suitable for reconfiguration word")

  val high_part_width = line_width - word_width
  val high_part_buffer = Reg(UInt(high_part_width.W))

  val word = Wire(UInt(word_width.W))
  word := io.in_v_bcast.asUInt

  when(io.rcgf.write_enable && io.rcgf.word_sel === 0.U) {
    high_part_buffer := word // the word gets truncated
  }

  cpg.io.write_enable := io.rcgf.write_enable && io.rcgf.word_sel === 1.U

  val full_line = Wire(UInt(line_width.W))
  full_line := Cat(high_part_buffer, word)
  //full_line(word_width - 1, 0) := word //low part
  //full_line(line_width - 1, word_width) := high_part_buffer //high part

  cpg.io.write_data := full_line.asTypeOf(new ControlPatternTableLine(interconnectConfig, controlPatternTableSize))
}
