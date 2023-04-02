# Модуль очереди

![Очень по-британски](item:tis3d:queue_module)

Модуль очереди способен хранить до шестнадцати (16) значений. Например, он может выступать в качестве расширенной памяти для [модулей выполнения](execution_module.md).

Пока модуль очереди не заполнен, он считывает значения со всех четырёх своих портов и помещает считанные значения в конец списка сохранённых значений. Пока модуль очереди не пуст, он записывает главное значение, т. е. значение, которое было первым передано во внутренний список значений, во все четыре своих порта. Другими словами, модуль очереди является буфером FIFO.

Значение из очереди всегда может быть передано только на один порт, т. е. значения никогда не будут дублироваться; даже если в одном цикле [контроллера](../block/controller.md) произойдёт многократное чтение, будет выполнено только одно. В отличие от [модуля стека](stack_module.md), модуль очереди не сбрасывает свои операции записи, когда в него записывается значение.