import { Anchor } from "antd";
import classes from "./scss/privacy-policy.module.css";
import type { AnchorLinkItemProps } from "antd/es/anchor/Anchor";
import type React from "react";

const PrivacyPolicy = () => {
  const getPolicyAnchorItem = (key: string, title: string): React.ReactNode => {
    return (
      <div className={classes.policy_anchor_item}>
        <div className={classes.item_num}>{key}</div>
        <div className={classes.item_title}>{title}</div>
      </div>
    );
  };

  const anchorItems: AnchorLinkItemProps[] = [
    {
      key: "1",
      href: "#information-we-collect",
      title: getPolicyAnchorItem("1", "Information We Collect"),
    },
    {
      key: "2",
      href: "#how-we-use-your-information",
      title: getPolicyAnchorItem("2", "How We Use Your Information"),
    },
    {
      key: "3",
      href: "#oauth-authentication",
      title: getPolicyAnchorItem("3", "OAuth & Authentication"),
    },
    {
      key: "4",
      href: "#data-storage-security",
      title: getPolicyAnchorItem("4", "Data Storage & Security"),
    },
    {
      key: "5",
      href: "#data-retention",
      title: getPolicyAnchorItem("5", "Data Retention"),
    },
    {
      key: "6",
      href: "#your-rights",
      title: getPolicyAnchorItem("6", "Your Rights"),
    },
    {
      key: "7",
      href: "#third-party-services",
      title: getPolicyAnchorItem("7", "Third-Party Services"),
    },
    {
      key: "8",
      href: "#changes-to-this-policy",
      title: getPolicyAnchorItem("8", "Changes to This Policy"),
    },
    {
      key: "9",
      href: "#contact-information",
      title: getPolicyAnchorItem("9", "Contact Information"),
    },
  ];

  return (
    <div className={classes.privacy_policy}>
      <div className={classes.content}>
        <div className={classes.header}>
          <h1>Privacy Policy</h1>
          <p>
            Trackera respects your privacy and is committed to protecting the personal information you share with us.
            This Privacy Policy explains what data we collect, how we use it, and your rights regarding your
            information.
          </p>
          <span>
            By using Trackera, you agree to the collection and use of information in accordance with this policy.
          </span>
        </div>
        <div className={classes.policy_items}>
          <section id="information-we-collect" className={classes.policy_item}>
            <h3 className={classes.policy_title}>1. Information We Collect</h3>
            <div className={classes.inner_policy}>
              <h4>1.1 Personal Information</h4>
              <p>When you sign in or use Trackera, we may collect the following information:</p>
              <ul>
                <li>Name</li>
                <li>Email address</li>
                <li>Profile picture (if provided by the authentication provider)</li>
                <li>Authentication identifiers from OAuth providers (e.g. Google)</li>
              </ul>
              <span>This information is collected only for authentication and user identification purposes.</span>
            </div>
            <div className={classes.inner_policy}>
              <h4>1.2 Worklog & Usage Data</h4>
              <p>Trackera allows you to manage and analyze worklogs. We may store:</p>
              <ul>
                <li>Worklog entries you create or upload</li>
                <li>Task identifiers and descriptions</li>
                <li>Time tracking data</li>
                <li>Uploaded files (e.g. Excel files for worklog imports)</li>
              </ul>
              <span>This data is user-provided and used solely to provide Trackera's core functionality.</span>
            </div>
            <div className={classes.inner_policy}>
              <h4>1.3 Third-Party Integrations (e.g. Jira)</h4>
              <p>If you connect third-party services such as Jira:</p>
              <ul>
                <li>
                  We access only the data required to perform the requested integration (e.g. syncing worklogs or
                  tasks).
                </li>
                <li>We do not access unrelated data from your third-party accounts.</li>
                <li>Access tokens are stored securely and can be revoked at any time.</li>
              </ul>
              <span>This data is user-provided and used solely to provide Trackera's core functionality.</span>
            </div>
          </section>
          <section id="how-we-use-your-information" className={classes.policy_item}>
            <h3 className={classes.policy_title}>2. How We Use Your Information</h3>
            <div className={classes.inner_policy}>
              <p>We use the collected information to:</p>
              <ul>
                <li>Authenticate users and manage accounts</li>
                <li>Provide and improve Trackera's features</li>
                <li>Sync data with connected third-party services</li>
                <li>Ensure application security and prevent abuse</li>
                <li>Analyze performance and fix technical issues</li>
              </ul>
              <span>We do not sell, rent, or trade your personal data.</span>
            </div>
          </section>
          <section id="oauth-authentication" className={classes.policy_item}>
            <h3 className={classes.policy_title}>3. OAuth & Authentication</h3>
            <div className={classes.inner_policy}>
              <p>Trackera uses OAuth providers (such as Google) for authentication.</p>
              <ul>
                <li>OAuth data is used only to identify and authenticate you</li>
                <li>We do not read private emails, contacts, or unrelated account data</li>
                <li>You may revoke access at any time via your OAuth provider's account settings</li>
              </ul>
            </div>
          </section>
          <section id="data-storage-security" className={classes.policy_item}>
            <h3 className={classes.policy_title}>4. Data Storage & Security</h3>
            <div className={classes.inner_policy}>
              <p>We take reasonable measures to protect your data, including:</p>
              <ul>
                <li>Secure storage of credentials and tokens</li>
                <li>Encrypted communication (HTTPS)</li>
                <li>Restricted access to sensitive information</li>
              </ul>
              <span>
                While no system is 100% secure, we strive to follow industry best practices to protect your data.
              </span>
            </div>
          </section>
          <section id="data-retention" className={classes.policy_item}>
            <h3 className={classes.policy_title}>5. Data Retention</h3>
            <div className={classes.inner_policy}>
              <ul>
                <li>Your data is retained as long as your account is active</li>
                <li>You may request deletion of your account and associated data</li>
                <li>Some technical logs may be retained temporarily for security or legal purposes</li>
              </ul>
            </div>
          </section>
          <section id="your-rights" className={classes.policy_item}>
            <h3 className={classes.policy_title}>6. Your Rights</h3>
            <div className={classes.inner_policy}>
              <p>Depending on your location, you may have the right to:</p>
              <ul>
                <li>Access your personal data</li>
                <li>Correct inaccurate data</li>
                <li>Request deletion of your data</li>
                <li>Withdraw consent for data processing</li>
              </ul>
              <span>You can exercise these rights by contacting us.</span>
            </div>
          </section>
          <section id="third-party-services" className={classes.policy_item}>
            <h3 className={classes.policy_title}>7. Third-Party Services</h3>
            <div className={classes.inner_policy}>
              <p>
                Trackera may link to or integrate with third-party services. We are not responsible for their privacy
                practices, and we encourage you to review their privacy policies.
              </p>
            </div>
          </section>
          <section id="changes-to-this-policy" className={classes.policy_item}>
            <h3 className={classes.policy_title}>8. Changes to This Policy</h3>
            <div className={classes.inner_policy}>
              <p>We may update this Privacy Policy from time to time.</p>
              <ul>
                <li>Any changes will be posted on this page</li>
                <li>Continued use of Trackera after updates means you accept the revised policy</li>
              </ul>
            </div>
          </section>
          <section id="contact-information" className={classes.policy_item}>
            <h3 className={classes.policy_title}>9. Contact Information</h3>
            <div className={classes.inner_policy}>
              <p>If you have any questions or concerns about this Privacy Policy, you can contact us at:</p>
              <ul>
                <li>Email: trackera@gmail.com</li>
              </ul>
            </div>
          </section>
        </div>
      </div>
      <div className={classes.anchor_container}>
        <div className={classes.header}>On This Page</div>
        <div className={classes.anchor_box}>
          <Anchor affix={false} items={anchorItems} classNames={{ item: classes.anchor_item }} />
        </div>
      </div>
    </div>
  );
};

export default PrivacyPolicy;
