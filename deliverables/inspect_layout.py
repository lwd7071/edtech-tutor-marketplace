import json
from docx import Document

doc = Document(r"D:\EdTech\deliverables\Tutor_Match_Thuyet_Minh_Du_An_UTE_SV_STARTUP_2026_Ban_Trinh_Bay_updated.docx")
rows = []
for index, paragraph in enumerate(doc.paragraphs[:35]):
    rows.append({
        "index": index,
        "text": paragraph.text,
        "page_break_before": paragraph.paragraph_format.page_break_before,
        "keep_with_next": paragraph.paragraph_format.keep_with_next,
        "keep_together": paragraph.paragraph_format.keep_together,
        "has_page_break": "w:type=\"page\"" in paragraph._p.xml,
    })
print(json.dumps(rows, ensure_ascii=True))
