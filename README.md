#ZpwTimePicker

[apk](https://github.com/daliammao/ZpwTimePicker/raw/master/app/apk/app-debug.apk)

Launching into **DatePicker**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Version 2 allows date-range selection using `options.setPickerType(Options.PickerType.BOTH)`. Picking date range is _one fluent gesture_ which begins with a long-press on the intended start-date, followed by a drag onto the intended end-date. The range can span as many days, months or years, as needed. During a drag, approaching the left/right edge of date picker scrolls the previous/next month into view.  

Date range selection:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_v2.png?raw=true" width="497" height="1000" />
</p>

Landscape:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_land_v2.png?raw=true" width="800" height="397" />
</p>

Date range selection spanning multiple months:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/date_picker_date_range_spanned_v2.png?raw=true" width="497" height="1000" />
</p>

Button at bottom-left corner can be used to switch to **TimePicker**: 

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/time_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Landscape:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/time_picker_land_v2.png?raw=true" width="800" height="397" />
</p>

The overflow button at top-right opens the **RecurrencePicker**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_picker_v2.png?raw=true" width="497" height="1000" />
</p>

Choosing **Custom...** from this menu brings you to **RecurrenceOptionCreator**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_option_creator_v2.png?raw=true" width="497" height="1000" />
</p>

Picking **Until a date** from the bottom spinner & clicking on the date shows a stripped down version of DatePicker:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/recurrence_option_creator_end_date_v2.png?raw=true" width="497" height="1000" />
</p>

**Sample Application**:

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_v2.png?raw=true" width="497" height="1000" />
</p>

Results of the selection in **sample application** (single date):

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_results_single_date_v2.png?raw=true" width="497" height="1000" />
</p>

Results of the selection in **sample application** (date range):

<p align="center">
<img src="https://github.com/vikramkakkar/SublimePicker/blob/master/img/sampler_results_date_range_v2.png?raw=true" width="497" height="1000" />
</p>

**How-to**s will be added in a day or two...

该项目参考自 [vikramkakkar/SublimePicker](https://github.com/vikramkakkar/SublimePicker)

由于我比较偏爱jodatime处理时间，所以该项目的时间交互，我全部换成了jodatime，而且对于时间段选择，我去掉了原有拖动进入时间区域选择状态，而是根据初始化传入的值来判断是否进行区域选择。我还使项目支持了timepicker的区域选择，修复了横屏展示不全的bug。