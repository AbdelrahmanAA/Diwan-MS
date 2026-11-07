/// Common values offered while typing. Only a typing aid: the fields stay free text. Medicine names should come
/// from the SFDA drug list once there is official access to it.
List<String> medicineSuggestions(bool arabic) => arabic ? _medicinesAr : _medicinesEn;

List<String> allergenSuggestions(bool arabic) => arabic ? _allergensAr : _allergensEn;

List<String> conditionSuggestions(bool arabic) => arabic ? _conditionsAr : _conditionsEn;

List<String> vaccineSuggestions(bool arabic) => arabic ? _vaccinesAr : _vaccinesEn;

const _medicinesEn = [
  'Paracetamol', 'Ibuprofen', 'Aspirin', 'Amoxicillin', 'Azithromycin', 'Metformin', 'Insulin', 'Atorvastatin',
  'Rosuvastatin', 'Amlodipine', 'Losartan', 'Bisoprolol', 'Omeprazole', 'Pantoprazole', 'Levothyroxine', 'Salbutamol',
  'Cetirizine', 'Loratadine', 'Vitamin D', 'Folic acid', 'Iron',
];

const _medicinesAr = [
  'باراسيتامول', 'إيبوبروفين', 'أسبرين', 'أموكسيسيلين', 'أزيثروميسين', 'ميتفورمين', 'إنسولين', 'أتورفاستاتين',
  'روزوفاستاتين', 'أملوديبين', 'لوسارتان', 'بيسوبرولول', 'أوميبرازول', 'بانتوبرازول', 'ليفوثيروكسين', 'سالبوتامول',
  'سيتريزين', 'لوراتادين', 'فيتامين د', 'حمض الفوليك', 'حديد',
];

const _allergensEn = [
  'Penicillin', 'Sulfa drugs', 'Aspirin', 'Ibuprofen', 'Peanuts', 'Tree nuts', 'Milk', 'Eggs', 'Wheat', 'Soy', 'Fish',
  'Shellfish', 'Sesame', 'Dust mites', 'Pollen', 'Mold', 'Pet dander', 'Latex', 'Bee stings',
];

const _allergensAr = [
  'بنسلين', 'أدوية السلفا', 'أسبرين', 'إيبوبروفين', 'فول سوداني', 'مكسرات', 'حليب', 'بيض', 'قمح', 'صويا', 'سمك',
  'قشريات بحرية', 'سمسم', 'عث الغبار', 'حبوب اللقاح', 'عفن', 'وبر الحيوانات', 'لاتكس', 'لسعات النحل',
];

const _conditionsEn = [
  'Type 1 diabetes', 'Type 2 diabetes', 'High blood pressure', 'High cholesterol', 'Asthma', 'Hypothyroidism',
  'Hyperthyroidism', 'Heart disease', 'Kidney disease', 'Anemia', 'Sickle cell disease', 'Thalassemia', 'Epilepsy',
  'Migraine', 'Arthritis', 'Depression', 'Cancer',
];

const _conditionsAr = [
  'السكري النوع الأول', 'السكري النوع الثاني', 'ارتفاع ضغط الدم', 'ارتفاع الكوليسترول', 'الربو', 'قصور الغدة الدرقية',
  'فرط نشاط الغدة الدرقية', 'أمراض القلب', 'أمراض الكلى', 'فقر الدم', 'فقر الدم المنجلي', 'الثلاسيميا', 'الصرع',
  'الصداع النصفي', 'التهاب المفاصل', 'الاكتئاب', 'السرطان',
];

const _vaccinesEn = [
  'Influenza', 'COVID-19', 'Meningococcal', 'Hepatitis A', 'Hepatitis B', 'Tetanus', 'Pneumococcal', 'MMR', 'Chickenpox',
  'HPV', 'Polio',
];

const _vaccinesAr = [
  'الإنفلونزا', 'كوفيد-19', 'الحمى الشوكية', 'التهاب الكبد أ', 'التهاب الكبد ب', 'الكزاز', 'المكورات الرئوية',
  'الحصبة والنكاف والحصبة الألمانية', 'جدري الماء', 'فيروس الورم الحليمي', 'شلل الأطفال',
];
