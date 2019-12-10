; addition_and_subtraction.s
; A simple demo of math on the 6502

start:
    ; demo addition
    clc
    lda #$F0
    adc #$20
    tax
    
    ; demo subtraction
    sec
    lda #$80
    sbc #$60    ; should set overflow flag

    brk
