package gemmini

import chisel3._
import chisel3.util._

class Sequencer[T <: Data](interconnectConfig : InterconnectConfig[T], sequenceTableSize: Int, controlPatternTableSize: Int, meshRows: Int, meshColumns: Int) extends Module { 
  val io = IO(new Bundle {
    //val pe_control = Output(new ModPEControl(interconnectConfig))
    val cycle_fire = Input(Bool())
    val sequencer_reset = Input(Bool())
    
    // which configuration pattern to use
    val next_control_pattern_indexes = Output(Vec(meshRows, Vec(meshColumns, UInt(log2Ceil(controlPatternTableSize).W))))

    // true when the configuration pattern to use changes
    val new_control_pattern = Output(Vec(meshRows, Vec(meshColumns, Bool())))

    // reconfiguration
    val write_enable = Input(Bool())
    val write_row = Input(UInt(log2Ceil(meshRows).W))
    val write_column = Input(UInt(log2Ceil(meshColumns).W))
    val write_index_element = Input(UInt(log2Ceil(sequenceTableSize).W))
    val write_data = Input(new SequenceTableLine(controlPatternTableSize))
  })


  // Global Fire Counter
  val fire_counter = RegInit(0.U(32.W))
  when(io.sequencer_reset) {
    fire_counter := 0.U
  }.elsewhen(io.cycle_fire) {
    fire_counter := fire_counter + 1.U
  }

  // Sequencinig elements
  val sequencing_elements: Seq[Seq[SequencingElement]] = Seq.fill(meshRows, meshColumns)(Module(new SequencingElement(sequenceTableSize, controlPatternTableSize)))
  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      sequencing_elements(r)(c).io.fire_counter := fire_counter
      sequencing_elements(r)(c).io.sequencer_reset := io.sequencer_reset
      io.next_control_pattern_indexes(r)(c) := sequencing_elements(r)(c).io.next_control_pattern_index
      io.new_control_pattern(r)(c) := sequencing_elements(r)(c).io.new_control_pattern
    }
  }

  // Reconfiguration
  for (r <- 0 until meshRows) {
    for (c <- 0 until meshColumns) {
      sequencing_elements(r)(c).io.write_enable := Mux(io.write_row === r.U && io.write_column === c.U, io.write_enable, false.B)
      sequencing_elements(r)(c).io.write_index := io.write_index_element
      sequencing_elements(r)(c).io.write_data := io.write_data
    }
  }
}
