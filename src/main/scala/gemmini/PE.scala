package gemmini

import chisel3._
import chisel3.util._
import chipsalliance.rocketchip.config

class TableReconfigurationControl(lines_number: Int, word_sel_width: Int) extends Bundle {
  val write_enable = Bool()
  val line_index = UInt(log2Ceil(lines_number).W)
  val word_sel = UInt(word_sel_width.W)
}

/**
  * PE contains CPG and ModPE
  * to reconfigure CPG, first write high part with word_sel = 0 and then, when when word_sel = 1 the low part combined with the previous high part are writen to the line
  */
class PE[T <: Data](interconnectConfig : InterconnectConfig[T], controlPatternTableSize: Int)
                   (implicit ev: Arithmetic[T]) extends Module { // Debugging variables
  import ev._

  val cpg_word_width = interconnectConfig.verticalBroadcastType.getWidth
  val cpg_line_width = (new ControlPatternTableLine(interconnectConfig, controlPatternTableSize)).getWidth
  val cpg_line_coalescer = Module(new LineCoalescer(cpg_word_width, cpg_line_width))

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
    val rcfg = Input(new TableReconfigurationControl(controlPatternTableSize, cpg_line_coalescer.word_sel_width))

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
  cpg.io.write_index := io.rcfg.line_index
  cpg.io.write_enable := cpg_line_coalescer.io.out.valid
  cpg.io.write_data := cpg_line_coalescer.io.out.bits.asTypeOf(new ControlPatternTableLine(interconnectConfig, controlPatternTableSize))

  cpg_line_coalescer.io.in.valid := io.rcfg.write_enable
  cpg_line_coalescer.io.in.bits := io.in_v_bcast.asUInt
  cpg_line_coalescer.io.word_select :=  io.rcfg.word_sel
}
