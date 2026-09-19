// max


    
import java.io.FileReader;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Parser {


    public Parser (String filename) {
        // init json parser
        Object json_parse = new JSONParser().parse(new FileReader(filename));
        JSONObject jo = (JSONObject) json_parse;
        JSONArray json = new JSONArray(jo.get("jobs"));

    }
}

/*
{
      "absolute_url": "https://boards.greenhouse.io/spacex/jobs/8643277002?gh_jid=8643277002",
      "data_compliance": [
        {
          "type": "gdpr",
          "requires_consent": false,
          "requires_processing_consent": false,
          "requires_retention_consent": false,
          "retention_period": null,
          "demographic_data_consent_applies": false
        }
      ],
      "education": "education_required",
      "internal_job_id": 6451875002,
      "location": {
        "name": "Hawthorne, CA"
      },
      "metadata": [
        {
          "id": 4010460002,
          "name": "Employment Type",
          "value": "Regular",
          "value_type": "single_select"
        },
        {
          "id": 4026103002,
          "name": "Discipline",
          "value": "Corporate - Finance & Accounting",
          "value_type": "single_select"
        },
        {
          "id": 28070425002,
          "name": "Program",
          "value": ["Corporate"],
          "value_type": "multi_select"
        },
        {
          "id": 4028954002,
          "name": "Description",
          "value": null,
          "value_type": "long_text"
        }
      ],
      "id": 8643277002,
      "updated_at": "2026-09-16T20:45:53-04:00",
      "requisition_id": "6451875002",
      "title": "Accountant",
      "company_name": "SpaceX",
      "first_published": "2026-07-21T19:49:28-04:00",
      "language": "en",
      "application_deadline": null,
      "content": "&lt;div class=&quot;content-intro&quot;&gt;&lt;p&gt;SpaceX was founded under the belief that a future where humanity is out exploring the stars is fundamentally more exciting than one where we are not. Today SpaceX is actively developing the technologies to make this possible, with the ultimate goal of&amp;nbsp;enabling human life on Mars.&lt;/p&gt;&lt;/div&gt;&lt;div&gt;\n&lt;p class=&quot;x_MsoNormal&quot;&gt;&lt;strong&gt;&lt;span data-olk-copy-source=&quot;MessageBody&quot;&gt;ACCOUNTANT&lt;/span&gt;&lt;/strong&gt;&lt;/p&gt;\n&lt;/div&gt;\n&lt;div&gt;\n&lt;p&gt;We are seeking detail-oriented, high-performing Accountants to join our Finance organization. Accountants will play a critical role in maintaining accurate financial records, supporting month-end close, variance analysis, reconciliations, and process improvements that enable SpaceX’s ambitious mission.&lt;/p&gt;\n&lt;p&gt;The ideal candidate excels in a dynamic, high-growth environment, while contributing to process improvements and collaborating effectively with team members.&lt;/p&gt;\n&lt;/div&gt;\n&lt;div&gt;\n&lt;p class=&quot;x_MsoNormal&quot;&gt;&lt;strong&gt;RESPONSIBILITIES:&lt;/strong&gt;&lt;/p&gt;\n&lt;/div&gt;\n&lt;ul&gt;\n&lt;li&gt;Perform month-end, quarter-end, and year-end close activities, including journal entries, account reconciliations, and variance analysis&lt;/li&gt;\n&lt;li&gt;Support inventory, fixed assets, cost accounting, or other specialized areas as assigned&lt;/li&gt;\n&lt;li&gt;Prepare monthly, quarterly, and annual divisional balance sheet reconciliations&lt;/li&gt;\n&lt;li&gt;Collaborate with accounting staff to foster a supportive work environment&lt;/li&gt;\n&lt;li&gt;Work closely with stakeholders in Tax, FP&amp;amp;A, Financial Reporting, and other business units to build and implement solutions that ensure seamless operations&lt;/li&gt;\n&lt;li&gt;Collaborate with auditors to provide accurate and timely submission of requested documentation&lt;/li&gt;\n&lt;li&gt;Perform ad hoc duties and analysis as assigned by management&lt;/li&gt;\n&lt;li&gt;Ensure compliance with US GAAP, company policies, and internal controls&lt;/li&gt;\n&lt;li&gt;Identify and implement process improvements, automation opportunities, and best practices&lt;/li&gt;\n&lt;/ul&gt;\n&lt;div&gt;\n&lt;p class=&quot;x_MsoNormal&quot;&gt;&lt;strong&gt;BASIC QUALIFICATIONS:&lt;/strong&gt;&lt;/p&gt;\n&lt;/div&gt;\n&lt;div&gt;\n&lt;ul&gt;\n&lt;li&gt;Bachelor’s degree&lt;/li&gt;\n&lt;li&gt;2+ years of accounting experience&lt;/li&gt;\n&lt;/ul&gt;\n&lt;/div&gt;\n&lt;div&gt;\n&lt;p class=&quot;x_MsoNormal&quot;&gt;&lt;strong&gt;PREFERRED SKILLS AND EXPERIENCE:&lt;/strong&gt;&lt;/p&gt;\n&lt;/div&gt;\n&lt;ul type=&quot;disc&quot;&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Bachelor’s degree in accounting, finance, business, or other business discipline&lt;/li&gt;\n&lt;li&gt;CPA license or equivalent&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;“Run it like you own it” mentality with the ability to thrive in a high paced, ever-changing environment&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Deadline focused and loves being in the office working face-to-face with teams&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Excellent technical, analytical, and communication skills, both written and verbal&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Proficient computer skills, particularly with Excel&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Ability to collaborate, guide junior staff, and contribute to team development&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Familiarity with SQL&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Understanding of US GAAP&lt;/li&gt;\n&lt;/ul&gt;\n&lt;div&gt;\n&lt;p class=&quot;x_MsoNormal&quot;&gt;&lt;strong&gt;ADDITIONAL REQUIREMENTS:&amp;nbsp;&lt;/strong&gt;&lt;/p&gt;\n&lt;/div&gt;\n&lt;ul type=&quot;disc&quot;&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;Willingness to work long hours and weekend when needed to meet critical deadlines&lt;/li&gt;\n&lt;li class=&quot;x_MsoNormal&quot;&gt;This position is based in Hawthorne, CA and requires being onsite - remote work not considered&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;&lt;strong&gt;COMPENSATION AND BENEFITS:&lt;/strong&gt;&lt;br&gt;&lt;br&gt;Pay Range:&lt;br&gt;Level 1: $85,000.00 - $115,000.00&lt;br&gt;Level 2: $100,000.00 - $140,000.00&lt;/p&gt;\n&lt;p&gt;Your actual level and base salary will be determined on a case-by-case basis and may vary based on the following considerations: job-related knowledge and skills, education, and experience.&lt;/p&gt;\n&lt;p&gt;Base salary is just one part of your total rewards package at SpaceX. You may also be eligible for long-term incentives, in the form of company stock or long-term cash awards, as well as potential discretionary bonuses and the ability to purchase additional stock at a discount through an Employee Stock Purchase Plan. You will also receive access to comprehensive medical, vision, and dental coverage, access to a 401(k) retirement plan, short and long-term disability insurance, life insurance, paid parental leave, and various other discounts and perks. You may also accrue 3 weeks of paid vacation and will be eligible for 10 or more paid holidays per year. Employees accrue paid sick leave pursuant to Company policy which satisfies or exceeds the accrual, carryover, and use requirements of the law.&lt;/p&gt;&lt;div class=&quot;content-conclusion&quot;&gt;&lt;p&gt;&lt;strong&gt;ITAR REQUIREMENTS:&lt;/strong&gt;&lt;/p&gt;\n&lt;ul&gt;\n&lt;li&gt;To conform to U.S. Government export regulations, applicant must be a (i) U.S. citizen or national, (ii) U.S. lawful, permanent resident (aka green card holder), (iii) Refugee under 8 U.S.C. § 1157, or (iv) Asylee under 8 U.S.C. § 1158, or be eligible to obtain the required authorizations from the U.S. Department of State. Learn more about the ITAR &lt;a href=&quot;https://www.pmddtc.state.gov/?id=ddtc_kb_article_page&amp;amp;sys_id=24d528fddbfc930044f9ff621f961987&quot;&gt;here&lt;/a&gt;. &amp;nbsp;&lt;/li&gt;\n&lt;/ul&gt;\n&lt;p&gt;SpaceX is an Equal Opportunity Employer; employment with SpaceX is governed on the basis of merit, competence and qualifications and will not be influenced in any manner by race, color, religion, gender, national origin/ethnicity, veteran status, disability status, age, sexual orientation, gender identity, marital status, mental or physical disability or any other legally protected status.&lt;/p&gt;\n&lt;p&gt;Applicants wishing to view a copy of SpaceX’s Affirmative Action Plan for veterans and individuals with disabilities, or applicants requiring reasonable accommodation to the application/interview process should reach out to&amp;nbsp;&lt;a href=&quot;mailto:EEOCompliance@spacex.com&quot;&gt;EEOCompliance@spacex.com&lt;/a&gt;&lt;em&gt;.&amp;nbsp;&lt;/em&gt;&lt;/p&gt;&lt;/div&gt;",
      "departments": [
        {
          "id": 4093729002,
          "name": "Finance",
          "child_ids": [],
          "parent_id": null
        }
      ],
      "offices": [
        {
          "id": 4000990002,
          "name": "Hawthorne, CA",
          "location": "Hawthorne, CA, United States",
          "child_ids": [],
          "parent_id": null
        },
        {
          "id": 4129138002,
          "name": "Palo Alto - 1530",
          "location": null,
          "child_ids": [],
          "parent_id": null
        }
      ],
      "ai_disclaimer": null,
      "include_ai_disclaimer": null,
      "ai_opt_out_request_url": null
    },
  */